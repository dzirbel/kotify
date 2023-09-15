package com.dzirbel.kotify.repository

import com.dzirbel.kotify.log.FakeLog
import com.dzirbel.kotify.repository.util.ToggleableState
import com.dzirbel.kotify.util.CurrentTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

open class FakeSavedRepository(savedStates: Map<String, Boolean> = emptyMap()) : SavedRepository {

    override val log = FakeLog<Repository.LogData>()

    private val savedStates: MutableMap<String, Boolean> = savedStates.toMutableMap()

    override val library: StateFlow<SavedRepository.Library?>
        get() {
            return MutableStateFlow(
                SavedRepository.Library(
                    ids = savedStates.filterValues { it }.keys,
                    cacheTime = CurrentTime.instant,
                ),
            )
        }

    override val libraryRefreshing: StateFlow<Boolean>
        get() = MutableStateFlow(false)

    override fun init() {}

    override fun refreshLibrary() {}

    override fun savedStateOf(id: String): StateFlow<ToggleableState<Boolean>?> {
        return MutableStateFlow(savedStates[id]?.let { ToggleableState.Set(it) })
    }

    override fun savedStatesOf(ids: Iterable<String>): List<StateFlow<ToggleableState<Boolean>?>> {
        return ids.map { savedStateOf(it) }
    }

    override fun setSaved(id: String, saved: Boolean) {
        savedStates[id] = saved
    }

    fun setSaved(ids: Iterable<String>, saved: Boolean = true) {
        ids.forEach { setSaved(it, saved) }
    }

    override fun invalidateUser() {
        savedStates.clear()
    }
}
