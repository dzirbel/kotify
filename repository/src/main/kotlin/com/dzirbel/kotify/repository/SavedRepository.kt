package com.dzirbel.kotify.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

/**
 * Manages the state of entities which can be saved/unsaved from a user's library.
 *
 * This interface specifies a generic repository which interfaces between a remote data source and a local cache, but is
 * itself implementation agnostic. It is itself a [Repository] for values of type [Boolean], i.e. whether or not an item
 * is saved.
 */
abstract class SavedRepository : Repository<Boolean>() {
    /**
     * Wraps the [result] of querying the saved state of [id].
     */
    data class QueryEvent(val id: String, val result: Boolean?)

    sealed class Event {
        /**
         * Emitted when cached saved states are queried, with [events] as the result.
         */
        data class QueryCached(val events: List<QueryEvent>) : Event()

        /**
         * Emitted when remote saved states are queried, with [events] as the result.
         */
        data class QueryRemote(val events: List<QueryEvent>) : Event()

        /**
         * Emitted when the saved state of [ids] is set to [saved].
         */
        data class SetSaved(val ids: List<String>, val saved: Boolean) : Event()

        /**
         * Emitted when the cached saved library is invalidated.
         */
        object InvalidateLibrary : Event()

        /**
         * Emitted when the cached saved library is queried, with [library] as the result.
         */
        data class QueryLibraryCached(val library: Set<String>?) : Event()

        /**
         * Emitted when the remote saved library is queried, with [library] as the result.
         */
        data class QueryLibraryRemote(val library: Set<String>) : Event()
    }

    /**
     * A [SharedFlow] of [Event]s which can be used to react to updates to the [SavedRepository].
     */
    abstract fun eventsFlow(): SharedFlow<Event>

    /**
     * Determines the [Instant] at which [id] was saved to the user's library, from the local cache. Returns null if it
     * is unsaved or its status is unknown.
     */
    abstract suspend fun savedTimeCached(id: String): Instant?

    /**
     * Returns a [StateFlow] reflecting the live state of the user's library as entity IDs.
     *
     * The returned [StateFlow] must be the same object between calls.
     *
     * If [allowCache] is true the library will be asynchronously loaded from the cache; if it is not in the cache or
     * [allowCache] is false and [allowRemote] is true it will then will be asynchronously loaded from the remote
     * source.
     *
     * The [scope] in which the library (either cached or from the remote) is loaded may be provided, but should
     * typically remain as its default value of [GlobalScope] to ensure the fetch is not cancelled. It is exposed mainly
     * to allow tests to provide their test scope.
     */
    abstract fun libraryState(
        allowCache: Boolean = true,
        allowRemote: Boolean = true,
        scope: CoroutineScope = GlobalScope,
    ): StateFlow<Set<String>?>

    /**
     * Saves the entity with the given [id] to the user's library, both via a remote call and in the local cache.
     */
    suspend fun save(id: String) = setSaved(id = id, saved = true)

    /**
     * Removes the entity with the given [id] from the user's library, both via a remote call and in the local cache.
     */
    suspend fun unsave(id: String) = setSaved(id = id, saved = false)

    /**
     * Saves the entities with the given [ids] to the user's library, both via a remote call and in the local cache.
     */
    suspend fun save(ids: List<String>) = setSaved(ids = ids, saved = true)

    /**
     * Removes the entities with the given [ids] from the user's library, both via a remote call and in the local cache.
     */
    suspend fun unsave(ids: List<String>) = setSaved(ids = ids, saved = false)

    /**
     * Adds or removes the entity with the given [id] from the user's library according to the given [saved] state, both
     * via a remote call and in the local cache.
     */
    suspend fun setSaved(id: String, saved: Boolean) = setSaved(ids = listOf(id), saved = saved)

    /**
     * Adds or removes the entities with the given [ids] from the user's library according to the given [saved] state,
     * both via a remote call and in the local cache.
     */
    abstract suspend fun setSaved(ids: List<String>, saved: Boolean)

    /**
     * Returns the last time the entire library state (i.e. set of all the user's saved entities) was updated, or null
     * if it has never been fetched.
     */
    abstract suspend fun libraryUpdated(): Instant?

    /**
     * Returns a [StateFlow] reflecting the live state of the last time the entire library was updated, or null if it
     * has never been fetched.
     *
     * The returned [StateFlow] must be the same object between calls.
     *
     * The [scope] in which the library state is loaded from the cache may be provided, but should typically remain as
     * its default value of [GlobalScope] to ensure the fetch is not cancelled. It is exposed mainly to allow tests to
     * provide their test scope.
     */
    abstract fun libraryUpdatedFlow(scope: CoroutineScope = GlobalScope): StateFlow<Instant?>

    /**
     * Invalidates the library state, i.e. the set of all the user's saved entities, in the local cache.
     */
    abstract suspend fun invalidateLibrary()

    /**
     * Gets the library of saved entity IDs from the local cache, or null if it has never been fetched in full.
     */
    abstract suspend fun getLibraryCached(): Set<String>?

    /**
     * Gets the library of saved entity IDs from the remote network, without checking its state in the local cache.
     */
    abstract suspend fun getLibraryRemote(): Set<String>

    /**
     * Retrieves the library of saved entity IDs, from the cache if it is present or from the remote network otherwise.
     */
    suspend fun getLibrary(): Set<String> = getLibraryCached() ?: getLibraryRemote()
}
