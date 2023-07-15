package com.dzirbel.kotify.repository2

import com.dzirbel.kotify.repository2.util.ToggleableState
import kotlinx.coroutines.flow.StateFlow

// TODO document
interface SavedRepository {
    val library: StateFlow<CacheState<Set<String>>?>

    /**
     * Initializes the [SavedRepository], typically loading the library. Invoked on application start but not in tests.
     */
    fun init()

    fun savedStateOf(id: String): StateFlow<ToggleableState<Boolean>?>
    fun savedStatesOf(ids: Iterable<String>): List<StateFlow<ToggleableState<Boolean>?>>

    // TODO remove a la Repository (and rework implementations somewhat)
    fun ensureSavedStateLoaded(id: String)

    fun refreshLibrary()

    fun save(id: String) = setSaved(id = id, saved = true)
    fun unsave(id: String) = setSaved(id = id, saved = false)
    fun setSaved(id: String, saved: Boolean)

    /**
     * Invalidates any local (on disk and in memory) state of the library, typically due to the user signing out.
     *
     * TODO have user-specific saved states on disk and only invalidate the in-memory states
     */
    suspend fun invalidate()
}
