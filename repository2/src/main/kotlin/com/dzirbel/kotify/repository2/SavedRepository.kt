package com.dzirbel.kotify.repository2

import com.dzirbel.kotify.repository2.util.ToggleableState
import kotlinx.coroutines.flow.StateFlow

// TODO document
interface SavedRepository {
    val library: StateFlow<CacheState<Set<String>>?>

    fun savedStateOf(id: String): StateFlow<ToggleableState<Boolean>?>
    fun savedStatesOf(ids: Iterable<String>): List<StateFlow<ToggleableState<Boolean>?>>

    // TODO remove a la Repository (and rework implementations somewhat)
    fun ensureSavedStateLoaded(id: String)

    fun refreshLibrary()

    fun save(id: String) = setSaved(id = id, saved = true)
    fun unsave(id: String) = setSaved(id = id, saved = false)
    fun setSaved(id: String, saved: Boolean)
}
