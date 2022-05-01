package com.dzirbel.kotify.repository

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.dzirbel.kotify.util.zipEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages access of entities [E] referenced by String IDs between a local cache and a remote, network-based source.
 */
abstract class Repository<E> {
    protected val states = ConcurrentHashMap<String, WeakReference<MutableState<E?>>>()

    /**
     * Retrieves the entity with the given [id] in the local cache, if it exists.
     */
    open suspend fun getCached(id: String): E? = getCached(ids = listOf(id))[0]

    /**
     * Retrieves the entities with the given [ids] in the local cache, if they exist.
     *
     * The returned list has the same order and length as [ids] with missing values as null values in the list.
     */
    abstract suspend fun getCached(ids: Iterable<String>): List<E?>

    /**
     * Retrieves the entity from the remote source for the given [id] without checking for a locally cached version,
     * saves it in the cache, and returns it.
     *
     * Implementations must update [states] with any new values.
     */
    open suspend fun getRemote(id: String): E? = getRemote(ids = listOf(id))[0]

    /**
     * Retrieves the entities from the the remote source for the given [ids] without checking for locally cached
     * versions, saves them in the cache, and returns them.
     *
     * The returned list has the same order and length as [ids] with missing values as null values in the list.
     *
     * Implementations must update [states] with any new values.
     */
    abstract suspend fun getRemote(ids: List<String>): List<E?>

    /**
     * Retrieves the entity for the given [id], from the local cache if [allowCache] is true and the cached value
     * satisfies [cachePredicate], otherwise fetches it from the remote source, caches, and returns it.
     */
    suspend fun get(
        id: String,
        allowCache: Boolean = true,
        cachePredicate: (id: String, cached: E) -> Boolean = { _, _ -> true },
    ): E? {
        if (allowCache) {
            getCached(id = id)
                ?.takeIf { cachePredicate(id, it) }
                ?.let { return it }
        }

        return getRemote(id = id)
    }

    /**
     * Retrieves the entities for the given [ids], from the local cache if [allowCache] is true and each satisfies
     * [cachePredicate], otherwise fetches the IDs not locally cached from the remote source, caches, and returns them.
     */
    suspend fun get(
        ids: List<String>,
        allowCache: Boolean = true,
        cachePredicate: (id: String, cached: E) -> Boolean = { _, _ -> true },
    ): List<E?> {
        if (!allowCache) {
            return getRemote(ids = ids)
        }

        val missingIndices = ArrayList<IndexedValue<String>>()

        val cachedValues = getCached(ids = ids)
            .mapIndexedTo(ArrayList(ids.size)) { index, cached ->
                val id = ids[index]
                val result = cached?.takeIf { cachePredicate(id, it) }

                if (result == null) {
                    missingIndices.add(IndexedValue(index = index, value = id))
                }

                result
            }

        if (missingIndices.isEmpty()) {
            return cachedValues
        }

        val remote = getRemote(ids = missingIndices.map { it.value })
        missingIndices.zipEach(remote) { indexedValue, value ->
            cachedValues[indexedValue.index] = value
        }

        return cachedValues
    }

    /**
     * Returns a [State] reflecting the live state of the entity with the given [id].
     *
     * The returned [State] is same object between calls for as long as it stays in context (i.e. is not
     * garbage-collected).
     *
     * If [fetchMissing] is true (the default), then the state will be fetched asynchronously if it is unknown.
     */
    suspend fun stateOf(id: String, fetchMissing: Boolean = true): State<E?> {
        states[id]?.get()?.let { return it }

        val cached = getCached(id)
        val state = mutableStateOf(cached)
        states[id] = WeakReference(state)

        if (cached == null && fetchMissing) {
            coroutineScope {
                launch { getRemote(id) }
            }
        }

        return state
    }

    /**
     * Returns [State]s reflecting the live state of the entities with the given [ids].
     *
     * The returned [State]s are the same objects between calls for as long as it stays in context (i.e. is not
     * garbage-collected).
     *
     * If [fetchMissing] is true (the default), then any unknown states will be fetched asynchronously.
     */
    suspend fun stateOf(ids: List<String>, fetchMissing: Boolean = true): List<State<E?>> {
        val missingIndices = ArrayList<IndexedValue<String>>()

        val existingStates = ids.mapIndexedTo(ArrayList(ids.size)) { index, id ->
            val state = states[id]?.get()
            if (state == null) {
                missingIndices.add(IndexedValue(index = index, value = id))
            }

            state
        }

        if (missingIndices.isEmpty()) {
            @Suppress("UNCHECKED_CAST")
            return existingStates as List<State<E?>>
        }

        val missingCached = getCached(ids = missingIndices.map { it.value })
        val idsToFetch = if (fetchMissing) mutableListOf<String>() else null

        missingIndices.zipEach(missingCached) { indexedValue, cached ->
            val state = mutableStateOf(cached)
            states[indexedValue.value] = WeakReference(state)
            existingStates[indexedValue.index] = state

            if (cached == null) {
                idsToFetch?.add(indexedValue.value)
            }
        }

        idsToFetch?.takeIf { it.isNotEmpty() }?.let {
            coroutineScope {
                launch { getRemote(ids = it) }
            }
        }

        @Suppress("UNCHECKED_CAST")
        return existingStates as List<State<E?>>
    }

    /**
     * Clears the cache of states used by [stateOf], for use in tests.
     */
    fun clearStates() {
        states.clear()
    }
}
