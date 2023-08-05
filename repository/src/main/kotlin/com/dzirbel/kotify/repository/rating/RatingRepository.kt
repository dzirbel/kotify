package com.dzirbel.kotify.repository.rating

import androidx.compose.runtime.Stable
import com.dzirbel.kotify.repository.user.UserRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Specifies the public API for repositories which manage [Rating] states.
 *
 * In practice this is always [TrackRatingRepository] since no other entities can be rated individually, but the API is
 * kept separate to decouple it from the implementation.
 */
@Stable
interface RatingRepository {
    /**
     * Returns a live [StateFlow] reflecting the current [Rating] (for the current user) of the entity with the given
     * [id]; null if the current user has not rated it.
     */
    fun ratingStateOf(id: String): StateFlow<Rating?>

    /**
     * Returns a batch of live [StateFlow]s reflecting the current [Rating]s (for the current user) of the entities with
     * the given [ids]; null if the current user has not rated it.
     *
     * The returned list has the same size and is in the same order as [ids].
     */
    fun ratingStatesOf(ids: Iterable<String>): List<StateFlow<Rating?>>

    /**
     * Returns a live [StateFlow] reflecting the current [AverageRating] (for the current user) of the entities with the
     * given [ids].
     */
    fun averageRatingStateOf(ids: Iterable<String>): StateFlow<AverageRating>

    /**
     * Asynchronously applies the given [rating] to the entity with the given [id].
     */
    fun rate(id: String, rating: Rating?)

    /**
     * Gets a snapshot (not live state) of the set of IDs of rated entities by the given [userId].
     *
     * This is mostly for debugging and exposing internal state of the repository.
     */
    suspend fun ratedEntities(userId: String = UserRepository.requireCurrentUserId): Set<String>

    /**
     * Clears all ratings (and their rating histories) for the user with the given [userId], or all users if null.
     */
    fun clearAllRatings(userId: String? = UserRepository.requireCurrentUserId)
}
