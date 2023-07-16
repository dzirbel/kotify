package com.dzirbel.kotify.repository2

import com.dzirbel.kotify.repository2.util.ToggleableState
import kotlinx.coroutines.flow.StateFlow

// TODO document
interface SavedRepository {
    val library: StateFlow<CacheState<Set<String>>?>

    /**
     * Initializes the [SavedRepository], typically loading the library from a local source.
     *
     * Invoked on application start but not in tests.
     */
    fun init()

    fun savedStateOf(id: String): StateFlow<ToggleableState<Boolean>?>
    fun savedStatesOf(ids: Iterable<String>): List<StateFlow<ToggleableState<Boolean>?>>

    fun refreshLibrary()

    fun save(id: String) = setSaved(id = id, saved = true)
    fun unsave(id: String) = setSaved(id = id, saved = false)
    fun setSaved(id: String, saved: Boolean)

    /**
     * Invalidates any local (on disk and in memory) state of the library specific to the current user, typically on log
     * out.
     */
    fun invalidateUser()
}
