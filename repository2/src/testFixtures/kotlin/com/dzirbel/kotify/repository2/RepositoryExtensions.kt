package com.dzirbel.kotify.repository2

import com.dzirbel.kotify.repository2.rating.Rating
import com.dzirbel.kotify.repository2.rating.RatingRepository
import com.dzirbel.kotify.repository2.util.ToggleableState
import io.mockk.every
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant

/**
 * Mocks calls to [Repository.stateOf] for the given [id] to return a [CacheState.Loaded] with the given [value] and
 * [cacheTime].
 */
fun <T> Repository<T>.mockStateCached(id: String, value: T, cacheTime: Instant) {
    every { stateOf(id = id) } returns MutableStateFlow(CacheState.Loaded(value, cacheTime))
}

/**
 * Mocks calls to [Repository.stateOf] for the given [id] to return null.
 */
fun <T> Repository<T>.mockStateNull(id: String) {
    every { stateOf(id = id) } returns MutableStateFlow(null)
}

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

/**
 * Mocks calls to [RatingRepository.ratingStateOf] for the given [id] to return the given [rating].
 */
fun RatingRepository.mockRating(id: String, rating: Rating?) {
    every { ratingStateOf(id = id) } returns MutableStateFlow(rating)
}

/**
 * Mocks calls to [RatingRepository.ratingStateOf] and [RatingRepository.ratingStatesOf] for the given [ids] to return
 * the respective [ratings].
 */
fun RatingRepository.mockRatings(ids: List<String>, ratings: List<Rating?>) {
    require(ids.size == ratings.size)

    val flows = ratings.map { MutableStateFlow(it) }
    every { ratingStatesOf(ids = ids) } returns flows
    every { ratingStateOf(id = any()) } answers { _ ->
        val id = firstArg<String>()
        val index = ids.indexOf(id).takeIf { it != -1 }
        requireNotNull(index?.let { flows.getOrNull(it) })
    }
}
