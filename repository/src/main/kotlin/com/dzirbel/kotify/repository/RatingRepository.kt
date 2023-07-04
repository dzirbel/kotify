package com.dzirbel.kotify.repository

import com.dzirbel.kotify.repository.user.UserRepository
import com.dzirbel.kotify.util.averageOrNull
import com.dzirbel.kotify.util.combineState
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

/**
 * Wrapper around a singe user-provided rating. To allow changing the rating format in the future, both the current
 * [rating] and the [maxRating] are included, as well as the [rateTime] when the rating was given.
 */
data class Rating(
    val rating: Int,
    val maxRating: Int = DEFAULT_MAX_RATING,
    val rateTime: Instant = Instant.now(),
) {
    val ratingPercent: Double
        get() = rating.toDouble() / maxRating

    /**
     * Calculates the relative rating scaled to the given [maxRating], e.g. if this [Rating] is 7/10 and the given
     * [maxRating] is 5, the returned value will be 3.5.
     */
    fun ratingRelativeToMax(maxRating: Int): Double {
        if (maxRating == this.maxRating) return rating.toDouble()
        @Suppress("UnnecessaryParentheses")
        return (maxRating.toDouble() / this.maxRating) * rating
    }

    companion object {
        /**
         * Default max value (number of stars) for individually rated items.
         */
        const val DEFAULT_MAX_RATING = 10

        /**
         * Default max value (number of stars) for ratings shown as averages.
         */
        const val DEFAULT_MAX_AVERAGE_RATING = 5
    }
}

/**
 * Represents a set of associated [ratings] (by their ID) which can be queried for the [averagePercent] rating of the
 * collection.
 */
data class AverageRating(val ratings: Map<String, Rating?>) {
    val averagePercent by lazy {
        ratings.values.averageOrNull { it.ratingPercent }
    }

    val numRatings by lazy {
        ratings.count { it.value != null }
    }

    constructor(ids: List<String>, ratings: Array<Rating?>) : this(
        ratings = ratings
            .withIndex()
            .associate { (index, rating) -> ids[index] to rating },
    )

    companion object {
        val empty = AverageRating(emptyMap())
    }
}

/**
 * Manages the state of entities which can have a user-provided [Rating].
 *
 * This interface specifies a generic repository for locally-stored rated values, with no remote component.
 */
interface RatingRepository {
    private fun requireUserId(): String = requireNotNull(UserRepository.currentUserId.cached) { "no user logged in" }

    /**
     * Retrieves the most recent [Rating] for the entity with the given [id] by the user with the given [userId]. This
     * is the canonical current rating for the entity.
     */
    suspend fun lastRatingOf(id: String, userId: String = requireUserId()): Rating?

    /**
     * Retrieves the most recent [Rating] for each of the entities with the given [ids] by the user with the given
     * [userId].
     */
    suspend fun lastRatingsOf(ids: List<String>, userId: String = requireUserId()): List<Rating?>

    /**
     * Retrieves the rating history of the entity with the given [id], i.e. all the ratings the user with the given
     * [userId] has given it, ordered by [Rating.rateTime], descending.
     */
    suspend fun allRatingsOf(id: String, userId: String = requireUserId()): List<Rating>

    /**
     * Submits a new user [rating] for the entity with the given [id] by the user with the given [userId]; if [rating]
     * is null all user ratings for the entity are removed.
     *
     * TODO probably retain previous ratings, but add a new "null" rating on top instead of clearing all
     */
    suspend fun rate(id: String, rating: Rating?, userId: String = requireUserId())

    /**
     * Returns the set of entity IDs which have a rating by the user with the given [userId].
     */
    suspend fun ratedEntities(userId: String = requireUserId()): Set<String>

    /**
     * Returns a [StateFlow] reflecting the live rating state of the entity with the given [id] for the user with the
     * given [userId].
     *
     * The returned [StateFlow] must be the same object between calls for as long as it stays in context (i.e. is not
     * garbage-collected).
     */
    fun ratingState(id: String, userId: String = requireUserId()): StateFlow<Rating?>

    /**
     * Returns [StateFlow]s reflecting the live rating states of the entities with the given [ids] for the user with the
     * given [userId].
     *
     * TODO avoid suspending
     */
    suspend fun ratingStates(ids: List<String>, userId: String = requireUserId()): List<StateFlow<Rating?>>

    /**
     * Returns a [StateFlow] reflecting the combined live [AverageRating] of the entities with the given [ids] for the
     * user with the given [userId].
     *
     * TODO avoid suspending
     */
    @Suppress("SuspendFunWithFlowReturnType")
    suspend fun averageRating(ids: List<String>, userId: String = requireUserId()): StateFlow<AverageRating> {
        return ratingStates(ids = ids, userId = userId)
            .combineState { ratingArray -> AverageRating(ids = ids, ratings = ratingArray) }
    }

    /**
     * Removes all ratings for all entities.
     *
     * If a [userId] is provided, only ratings by that user are cleared.
     */
    suspend fun clearAllRatings(userId: String?)
}
