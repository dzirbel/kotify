package com.dzirbel.kotify.repository

import com.dzirbel.kotify.repository.util.ToggleableState
import com.dzirbel.kotify.util.CurrentTime
import io.mockk.every
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant

/**
 * Mocks calls to [SavedRepository.savedStateOf] for the given [id] to return [ToggleableState.Set] of [saved] if not
 * null, else null.
 */
fun SavedRepository.mockSaveState(id: String, saved: Boolean?) {
    every { savedStateOf(id = id) } returns MutableStateFlow(saved?.let { ToggleableState.Set(saved) })
}

/**
 * Mocks calls to [SavedRepository.savedStateOf] and [SavedRepository.savedStatesOf] for the given [ids] to return
 * [ToggleableState.Set] of the respective values of [saved] when not null, else null.
 */
fun SavedRepository.mockSaveStates(ids: List<String>, saved: List<Boolean?>) {
    require(ids.size == saved.size)

    val flows = saved.map { s -> MutableStateFlow(s?.let { ToggleableState.Set(it) }) }
    every { savedStatesOf(ids = ids) } returns flows
    every { savedStateOf(id = any()) } answers { _ ->
        val id = firstArg<String>()
        val index = ids.indexOf(id).takeIf { it != -1 }
        requireNotNull(index?.let { flows.getOrNull(it) })
    }
}

fun SavedRepository.mockLibrary(ids: Set<String>?, cacheTime: Instant = CurrentTime.instant) {
    val flows = mutableMapOf<String, MutableStateFlow<ToggleableState<Boolean>?>>()
    val libraryFlow = MutableStateFlow(ids?.let { SavedRepository.Library(ids, cacheTime) })

    every { library } returns libraryFlow
    every { libraryRefreshing } returns MutableStateFlow(false)

    every { savedStatesOf(ids = any()) } answers {
        val argIds = firstArg<Iterable<String>>()
        argIds.map { id ->
            flows.getOrPut(id) { MutableStateFlow(ids?.let { ToggleableState.Set(id in ids) }) }
        }
    }

    every { savedStateOf(id = any()) } answers {
        val id = firstArg<String>()
        flows.getOrPut(id) { MutableStateFlow(ids?.let { ToggleableState.Set(id in ids) }) }
    }
}
