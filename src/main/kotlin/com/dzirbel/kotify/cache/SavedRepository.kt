package com.dzirbel.kotify.cache

import androidx.compose.runtime.State
import kotlinx.coroutines.flow.SharedFlow
import java.time.Instant

/**
 * Manages the state of entities which can be saved/unsaved from a user's library.
 *
 * This interface specifies a generic repository which interfaces between a remote data source and a local cache, but is
 * itself implementation agnostic.
 */
interface SavedRepository {
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
    fun eventsFlow(): SharedFlow<Event>

    /**
     * Determines whether [id] has been saved to the user's library, from the local cache. Returns null if its status is
     * not cached.
     */
    suspend fun isSavedCached(id: String): Boolean? = isSavedCached(ids = listOf(id))[0]

    /**
     * Determines whether each of [ids] has been saved to the user's library, from the local cache. Returns null for
     * each if its status is not cached.
     */
    suspend fun isSavedCached(ids: List<String>): List<Boolean?>

    /**
     * Retrieves the saved state from the remote source for the given [id] without checking for a locally cached state,
     * saves it to the cache, and returns it.
     */
    suspend fun isSavedRemote(id: String): Boolean

    /**
     * Retrieves the saved state for the given [id], from the local cache if present, otherwise fetches it from the
     * remote source, caches, and returns it.
     */
    suspend fun isSaved(id: String): Boolean = isSavedCached(id) ?: isSavedRemote(id)

    /**
     * Returns a [State] reflecting the live saved state of the entity with the given [id].
     *
     * The returned [State] must be the same object between calls for as long as it stays in context (i.e. is not
     * garbage-collected).
     *
     * If [fetchIfUnknown] is true, the saved state will be fetched from the remote state if it is not cached (i.e.
     * null). Otherwise, the returned state may have a null value.
     */
    suspend fun savedStateOf(id: String, fetchIfUnknown: Boolean = false): State<Boolean?>

    /**
     * Returns a [State] reflecting the live state of the user's library as entity IDs.
     *
     * The returned [State] must be the same object between calls.
     *
     * If [fetchIfUnknown] is true, the library state will be fetched from the remote data source if it is not cached
     * (i.e. null). Otherwise, the returned state may have a null value.
     *
     * TODO unit test
     */
    suspend fun libraryState(fetchIfUnknown: Boolean = false): State<Set<String>?>

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
    suspend fun setSaved(ids: List<String>, saved: Boolean)

    /**
     * Returns the last time the entire library state (i.e. set of all the user's saved entities) was updated, or null
     * if it has never been fetched.
     */
    suspend fun libraryUpdated(): Instant?

    /**
     * Invalidates the library state, i.e. the set of all the user's saved entities, in the local cache.
     */
    suspend fun invalidateLibrary()

    /**
     * Gets the library of saved entity IDs from the local cache, or null if it has never been fetched in full.
     */
    suspend fun getLibraryCached(): Set<String>?

    /**
     * Gets the library of saved entity IDs from the remote network, without checking its state in the local cache.
     */
    suspend fun getLibraryRemote(): Set<String>

    /**
     * Retrieves the library of saved entity IDs, from the cache if it is present or from the remote network otherwise.
     */
    suspend fun getLibrary(): Set<String> = getLibraryCached() ?: getLibraryRemote()
}
