package com.dzirbel.kotify.repository

import com.dzirbel.kotify.util.zipEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages access of entities [E] referenced by String IDs between a local cache and a remote, network-based source.
 */
abstract class Repository<E> {
    private val states = ConcurrentHashMap<String, WeakReference<MutableStateFlow<E?>>>()

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

    /**
     * Updates the live state accessed in [stateOf] for the entity with the given [id] to the given [value], if it is
     * currently being tracked.
     */
    fun updateLiveState(id: String, value: E?) {
        states[id]?.get()?.value = value
    }

    /**
     * Updates all the live states accessed in [stateOf] currently being tracked according to the given mapping
     * function.
     */
    fun updateLiveStates(updatedValue: (id: String) -> E?) {
        synchronized(states) {
            for ((id, reference) in states) {
                reference.get()?.value = updatedValue(id)
            }
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
     * Returns a [StateFlow] reflecting the live state of the entity with the given [id].
     *
     * If the state has not been previously tracked it is asynchronously initialized from the cached value if
     * [allowCache] is true, then if either [allowCache] was false or there was no cached value from the remote if
     * [allowRemote] is true. If neither [allowCache] or [allowRemote] are true an [IllegalArgumentException] is thrown.
     *
     * If the state has already been tracked, it must reflect at least the current cached value and as such [allowCache]
     * is irrelevant. If the state exists but is null (i.e. there is no cached value) and [allowRemote] is true it is
     * asynchronously initialized from the remote.
     *
     * In either case, as soon as an initial value for the state is established [onStateInitialized] is invoked with it
     * as the argument.
     *
     * The returned [StateFlow] is same object between calls for as long as it stays in context (i.e. is not
     * garbage-collected).
     */
    fun stateOf(
        id: String,
        scope: CoroutineScope = GlobalScope,
        allowCache: Boolean = true,
        allowRemote: Boolean = true,
        onStateInitialized: (E?) -> Unit = {},
    ): StateFlow<E?> {
        require(allowCache || allowRemote) { "must allow either cache or remote source" }

        val state: MutableStateFlow<E?>
        synchronized(states) {
            states[id]?.get()?.let { state ->
                if (allowRemote && state.value == null) {
                    scope.launch {
                        val value = getRemote(id)
                        onStateInitialized(value)
                    }
                } else {
                    onStateInitialized(state.value)
                }

                return state
            }

            state = MutableStateFlow(null)
            states[id] = WeakReference(state)
        }

        scope.launch {
            val value = if (allowRemote) get(id = id, allowCache = allowCache) else getCached(id = id)
            state.value = value
            onStateInitialized(value)
        }

        return state
    }

    /**
     * Returns [StateFlow]s reflecting the live states of the entities with the given [ids].
     *
     * For each entity, if the state has not been previously tracked it is asynchronously initialized from the cached
     * value if [allowCache] is true, then if either [allowCache] was false or there was no cached value from the remote
     * if [allowRemote] is true. If neither [allowCache] or [allowRemote] are true an [IllegalArgumentException] is
     * thrown.
     *
     * If the state has already been tracked, it must reflect at least the current cached value and as such [allowCache]
     * is irrelevant. If the state exists but is null (i.e. there is no cached value) and [allowRemote] is true it is
     * asynchronously initialized from the remote.
     *
     * The returned [StateFlow]s are the same objects between calls for as long as they stay in context (i.e. are not
     * garbage-collected).
     */
    fun statesOf(
        ids: List<String>,
        scope: CoroutineScope = GlobalScope,
        allowCache: Boolean = true,
        allowRemote: Boolean = true,
    ): List<StateFlow<E?>> {
        require(allowCache || allowRemote) { "must allow either cache or remote source" }

        val missingIndices = ArrayList<IndexedValue<String>>()
        val idsToFetchFromRemote = if (allowRemote) mutableListOf<String>() else null

        // retrieve or create new flows for each of the ids
        val returnStates = synchronized(states) {
            ids.mapIndexedTo(ArrayList(ids.size)) { index, id ->
                val existingState = states[id]?.get()
                if (existingState != null) {
                    if (allowRemote && existingState.value == null) {
                        idsToFetchFromRemote?.add(id)
                    }

                    existingState
                } else {
                    missingIndices.add(IndexedValue(index = index, value = id))

                    val state = MutableStateFlow<E?>(null)
                    states[id] = WeakReference(state)
                    state
                }
            }
        }

        val missingIds = missingIndices.map { it.value }
        when {
            // if we allow the cache and have states which were just created, try to load them from the cache and then
            // fall back to loading anything which is still missing (newly created state or not) from the remote, if
            // allowed
            allowCache && missingIndices.isNotEmpty() -> {
                scope.launch {
                    val missingCached = getCached(ids = missingIds)

                    missingIndices.zipEach(missingCached) { indexedValue, cached ->
                        states[indexedValue.value]?.get()?.value = cached
                        if (cached == null) {
                            // if any value are missing from the cache, add to batch to fetch from the remote (only has
                            // an effect if fetchMissing is true)
                            idsToFetchFromRemote?.add(indexedValue.value)
                        }
                    }

                    idsToFetchFromRemote?.let { getRemote(ids = it) }
                }
            }

            // otherwise, if we allow the remote then do a batch fetch for any state which did not have a value
            allowRemote -> {
                requireNotNull(idsToFetchFromRemote)
                idsToFetchFromRemote.addAll(missingIds)

                scope.launch {
                    getRemote(ids = idsToFetchFromRemote)
                }
            }
        }

        return returnStates
    }

    /**
     * Clears the cache of states used by [statesOf], for use in tests.
     */
    fun clearStates() {
        synchronized(states) {
            states.clear()
        }
    }
}
