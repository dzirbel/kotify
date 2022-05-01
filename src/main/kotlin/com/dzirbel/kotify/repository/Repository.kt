package com.dzirbel.kotify.repository

import com.dzirbel.kotify.util.zipEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages access of entities [E] referenced by String IDs between a local cache and a remote, network-based source.
 */
abstract class Repository<E> {
    private val flows = ConcurrentHashMap<String, WeakReference<MutableStateFlow<E?>>>()

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
     * Implementations must call [updateLiveState] with any new values.
     */
    open suspend fun getRemote(id: String): E? = getRemote(ids = listOf(id))[0]

    /**
     * Retrieves the entities from the the remote source for the given [ids] without checking for locally cached
     * versions, saves them in the cache, and returns them.
     *
     * The returned list has the same order and length as [ids] with missing values as null values in the list.
     *
     * Implementations must call [updateLiveState] with any new values.
     */
    abstract suspend fun getRemote(ids: List<String>): List<E?>

    // TODO document
    fun updateLiveState(id: String, value: E?) {
        flows[id]?.get()?.value = value
    }

    // TODO document
    fun updateLiveStates(updatedValue: (id: String) -> E?) {
        for ((id, reference) in flows) {
            reference.get()?.value = updatedValue(id)
        }
    }

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
     * Hack for [flowOf] to allow provided a default value for initState; as of Kotlin 1.6.10 providing a default value
     * of suspend functions (at least in some cases) causes an internal compiler error.
     */
    @Suppress("SuspendFunWithFlowReturnType") // TODO re-evaluate
    suspend fun flowOf(id: String, fetchMissing: Boolean = true): StateFlow<E?> {
        return flowOf(id = id, fetchMissing = fetchMissing, initState = { getCached(id) })
    }

    /**
     * Returns a [StateFlow] reflecting the live state of the entity with the given [id], with initial value provided by
     * [initState], by default loading it from the cache.
     *
     * The returned [StateFlow] is same object between calls for as long as it stays in context (i.e. is not
     * garbage-collected).
     *
     * If [fetchMissing] is true (the default) the state will be fetched asynchronously if it is unknown.
     */
    @Suppress("SuspendFunWithFlowReturnType") // TODO re-evaluate
    suspend fun flowOf(
        id: String,
        fetchMissing: Boolean = true,
        initState: suspend Repository<E>.(id: String) -> E?,
    ): StateFlow<E?> {
        flows[id]?.get()?.let { flow ->
            if (flow.value == null && fetchMissing) {
                coroutineScope {
                    launch { getRemote(id) }
                }
            }

            return flow
        }

        val cached = initState(id)
        val flow = MutableStateFlow(cached)
        flows[id] = WeakReference(flow)

        if (cached == null && fetchMissing) {
            coroutineScope {
                launch { getRemote(id) }
            }
        }

        return flow
    }

    /**
     * Returns [StateFlow]s reflecting the live state of the entities with the given [ids].
     *
     * The returned [StateFlow]s are the same objects between calls for as long as it stays in context (i.e. is not
     * garbage-collected).
     *
     * If [fetchMissing] is true (the default), then any unknown states will be fetched asynchronously.
     *
     * TODO asynchronously fetch missing values even if the flows already exist, to match singular version
     */
    suspend fun flowOf(ids: List<String>, fetchMissing: Boolean = true): List<StateFlow<E?>> {
        val missingIndices = ArrayList<IndexedValue<String>>()

        val existingFlows = ids.mapIndexedTo(ArrayList(ids.size)) { index, id ->
            val flow = flows[id]?.get()
            if (flow == null) {
                missingIndices.add(IndexedValue(index = index, value = id))
            }

            flow
        }

        if (missingIndices.isEmpty()) {
            @Suppress("UNCHECKED_CAST")
            return existingFlows as List<StateFlow<E?>>
        }

        val missingCached = getCached(ids = missingIndices.map { it.value })
        val idsToFetch = if (fetchMissing) mutableListOf<String>() else null

        missingIndices.zipEach(missingCached) { indexedValue, cached ->
            val flow = MutableStateFlow(cached)
            flows[indexedValue.value] = WeakReference(flow)
            existingFlows[indexedValue.index] = flow

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
        return existingFlows as List<StateFlow<E?>>
    }

    /**
     * Clears the cache of states used by [flowOf], for use in tests.
     */
    fun clearFlows() {
        flows.clear()
    }
}
