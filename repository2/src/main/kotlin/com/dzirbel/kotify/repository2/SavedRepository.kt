package com.dzirbel.kotify.repository2

import com.dzirbel.kotify.repository2.util.ToggleableState
import kotlinx.coroutines.flow.StateFlow

/**
 * Handles logic to manage the save states for a set of entities with String (ID) keys.
 *
 * TODO expose save times and save check times
 */
interface SavedRepository {
    /**
     * Reflects the current state of the entity library of saved entities, if it is available.
     *
     * Only provided when the entire library has been fetched (e.g. the entire set of a user's followed artists) rather
     * than just individual states.
     */
    val library: StateFlow<CacheState<Set<String>>?>

    /**
     * Initializes the [SavedRepository], typically loading the library from a local source.
     *
     * Invoked on application start but not in tests.
     */
    fun init()

    /**
     * Retrieves a [StateFlow] which reflects the live [ToggleableState] of the save state for the entity with the given
     * [id].
     *
     * Follows a similar pattern to [Repository.stateOf] and the same guarantees are provided.
     */
    fun savedStateOf(id: String): StateFlow<ToggleableState<Boolean>?>

    /**
     * Retrieves a bath of [StateFlow]s which reflect the respective live [ToggleableState]s of the save states for the
     * entities with the given [ids].
     */
    fun savedStatesOf(ids: Iterable<String>): List<StateFlow<ToggleableState<Boolean>?>>

    /**
     * Asynchronously refreshes the state of the [library] (and all individual saved states) from the remote data
     * source.
     */
    fun refreshLibrary()

    /**
     * Sets the saved state of the entity with the given [id] to [saved].
     *
     * TODO batch set saved
     */
    fun setSaved(id: String, saved: Boolean)

    /**
     * Ensures the entity with the given [id] is marked as saved.
     */
    fun save(id: String) = setSaved(id = id, saved = true)

    /**
     * Ensures the entity with the given [id] is not marked as saved.
     */
    fun unsave(id: String) = setSaved(id = id, saved = false)

    /**
     * Invalidates any local (on disk and in memory) state of the library specific to the current user, typically on log
     * out.
     */
    fun invalidateUser()
}
