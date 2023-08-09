package com.dzirbel.kotify.repository

import io.mockk.every
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant

/**
 * Mocks calls to [Repository.stateOf] for the given [id] to return a [CacheState.Loaded] with the given [value] and
 * [cacheTime].
 */
fun <T> Repository<T>.mockStateCached(id: String, value: T, cacheTime: Instant) {
    every { stateOf(id = id, cacheStrategy = any()) } returns
        MutableStateFlow(CacheState.Loaded(value, cacheTime))
}

fun <T> Repository<T>.mockStates(ids: List<String>, values: List<T>, cacheTime: Instant) {
    every { statesOf(ids = match { it.toList() == ids }, cacheStrategy = any()) } returns
        values.map { MutableStateFlow(CacheState.Loaded(it, cacheTime)) }
}

/**
 * Mocks calls to [Repository.stateOf] for the given [id] to return null.
 */
fun <T> Repository<T>.mockStateNull(id: String) {
    every { stateOf(id = id, cacheStrategy = any()) } returns MutableStateFlow(null)
}
