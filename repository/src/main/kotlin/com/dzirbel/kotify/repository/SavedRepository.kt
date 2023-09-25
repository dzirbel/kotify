package com.dzirbel.kotify.repository

import androidx.compose.runtime.Stable
import com.dzirbel.kotify.log.Logging
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

/**
 * Handles logic to manage the save states for a set of entities with String (ID) keys.
 *
 * TODO expose save times and save check times
 * TODO use CacheStrategy
 */
@Stable
interface SavedRepository : Logging<Repository.LogData> {
    /**
     * Wraps the state of a saved library; a set of [ids] and an [Instant] at the last time it was fetched from the
     * remote data source.
     */
    @Stable
    data class Library(
        val ids: Set<String>,
        val cacheTime: Instant,
    ) {
        /**
         * Returns a [Library] adding the given [ids], and this [Library]'s [cacheTime].
         */
        fun plus(ids: Set<String>?): Library {
            return if (ids.isNullOrEmpty()) this else copy(ids = this.ids + ids)
        }
    }

    /**
     * Wraps the state of a saved entity.
     */
    sealed interface SaveState {
        val saved: Boolean?

        /**
         * The [saved] state is known, with an optional [saveTime] when the save state was last updated (but may be
         * null, e.g. if the save state was fetched from the API and was not provided).
         */
        data class Set(override val saved: Boolean, val saveTime: Instant? = null) : SaveState

        /**
         * The [saved] state is in the process of being set.
         */
        data class Setting(override val saved: Boolean) : SaveState

        /**
         * The remote data source could not determine the saved state of the entity.
         */
        data object NotFound : SaveState {
            override val saved
                get() = null
        }

        /**
         * An error occurred while setting or loading the [saved] state.
         */
        data class Error(val throwable: Throwable? = null) : SaveState {
            override val saved
                get() = null
        }
    }

    /**
     * Reflects the current state of the entity library of saved entities, if it is available.
     *
     * Only provided when the entire library has been fetched (e.g. the entire set of a user's followed artists) rather
     * than just individual states.
     */
    val library: StateFlow<Library?>

    /**
     * Reflects whether the [library] is currently being refreshed, either as a first load or subsequently via
     * [refreshLibrary].
     */
    val libraryRefreshing: StateFlow<Boolean>

    /**
     * A user-readable name for the type of saved entity stored in this repository, e.g. "artist".
     */
    val entityName: String

    /**
     * Initializes the [SavedRepository], typically loading the library from a local source.
     *
     * Should be invoked on application start and when a user is first signed in, but not in tests.
     */
    fun init()

    /**
     * Asynchronously refreshes the state of the [library] (and all individual saved states) from the remote data
     * source.
     */
    fun refreshLibrary()

    /**
     * Retrieves a [StateFlow] which reflects the live [SaveState] of the save state for the entity with the given [id].
     *
     * Follows a similar pattern to [Repository.stateOf] and the same guarantees are provided.
     */
    fun savedStateOf(id: String): StateFlow<SaveState?>

    /**
     * Retrieves a bath of [StateFlow]s which reflect the respective live [SaveState]s of the save states for the
     * entities with the given [ids].
     */
    fun savedStatesOf(ids: Iterable<String>): List<StateFlow<SaveState?>>

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
