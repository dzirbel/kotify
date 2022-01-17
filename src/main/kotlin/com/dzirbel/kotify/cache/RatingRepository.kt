package com.dzirbel.kotify.cache

import androidx.compose.runtime.State
import java.time.Instant

/**
 * Wrapper around a singe user-provided rating. To allow changing the rating format in the future, both the current
 * [rating] and the [maxRating] are included, as well as the [rateTime] when the rating was given.
 */
data class Rating(val rating: Int, val maxRating: Int = DEFAULT_MAX_RATING, val rateTime: Instant = Instant.now()) {
    val ratingPercent: Double
        get() = rating.toDouble() / maxRating

    companion object {
        const val DEFAULT_MAX_RATING = 10
    }
}

/**
 * Manages the state of entities which can have a user-provided [Rating].
 *
 * This interface specifies a generic repository for locally-stored rated values, with no remote component.
 */
interface RatingRepository {
    /**
     * Retrieves the most recent [Rating] for the entity with the given [id]. This is the canonical current rating for
     * the entity.
     */
    suspend fun lastRatingOf(id: String): Rating?

    /**
     * Retrieves the rating history of the entity with the given [id], i.e. all the ratings the user has given it,
     * ordered by [Rating.rateTime], descending.
     */
    suspend fun allRatingsOf(id: String): List<Rating>

    /**
     * Submits a new user [rating] for the entity with the given [id]; if [rating] is null all user ratings for the
     * entity are removed.
     *
     * TODO probably retain previous ratings, but add a new "null" rating on top instead of clearing all
     */
    suspend fun rate(id: String, rating: Rating?)

    /**
     * Returns the set of entity IDs which have a rating.
     */
    suspend fun ratedEntities(): Set<String>

    /**
     * Returns a [State] reflecting the live rating state of the entity with the given [id].
     *
     * The returned [State] must be the same object between calls for as long as it stays in context (i.e. is not
     * garbage-collected).
     */
    suspend fun ratingState(id: String): State<Rating?>

    /**
     * Removes all ratings for all entities.
     */
    suspend fun clearAllRatings()
}
