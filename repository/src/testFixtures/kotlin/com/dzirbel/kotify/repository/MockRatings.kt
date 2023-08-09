package com.dzirbel.kotify.repository

import com.dzirbel.kotify.repository.rating.Rating
import com.dzirbel.kotify.repository.rating.RatingRepository
import io.mockk.every
import kotlinx.coroutines.flow.MutableStateFlow

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
