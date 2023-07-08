package com.dzirbel.kotify.repository2

import com.dzirbel.kotify.repository.player.ToggleableState
import kotlinx.coroutines.flow.StateFlow

// TODO document
interface SavedRepository {
    val library: StateFlow<CacheState<Set<String>>?>

    fun savedStateOf(id: String): StateFlow<ToggleableState<Boolean>?>

    fun ensureSavedStateLoaded(id: String)
    // TODO fun ensureSavedStatesLoaded(ids: Iterable<String>)

    fun refreshLibrary()

    fun save(id: String) = setSaved(id = id, saved = true)
    fun unsave(id: String) = setSaved(id = id, saved = false)
    fun setSaved(id: String, saved: Boolean)
}
