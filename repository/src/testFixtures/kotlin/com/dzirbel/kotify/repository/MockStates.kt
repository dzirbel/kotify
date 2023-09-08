package com.dzirbel.kotify.repository

import com.dzirbel.kotify.util.CurrentTime
import io.mockk.every
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant

/**
 * Mocks calls to [Repository.stateOf] for the given [id] to return a [CacheState.Loaded] with the given [value] and
 * [cacheTime].
 */
fun <T> Repository<T>.mockStateCached(id: String, value: T, cacheTime: Instant = CurrentTime.instant) {
    every { stateOf(id = id, cacheStrategy = any()) } returns
        MutableStateFlow(CacheState.Loaded(value, cacheTime))
}

fun <T> Repository<T>.mockStates(ids: List<String>, values: List<T>, cacheTime: Instant = CurrentTime.instant) {
    require(ids.size == values.size)

    every { statesOf(ids = match { it.toSet() == ids.toSet() }, cacheStrategy = any()) } answers {
        // re-order values in the order they were requested
        val givenIds = firstArg<Iterable<String>>()
        givenIds.map { id ->
            val index = ids.indexOf(id).takeIf { it != -1 }
            requireNotNull(index) { "no value provided for $id" }
            MutableStateFlow(CacheState.Loaded(values[index], cacheTime))
        }
    }
}

/**
 * Mocks calls to [Repository.stateOf] for the given [id] to return null.
 */
fun <T> Repository<T>.mockStateNull(id: String) {
    every { stateOf(id = id, cacheStrategy = any()) } returns MutableStateFlow(null)
}
