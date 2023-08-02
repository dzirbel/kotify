package com.dzirbel.kotify.repository.rating

import com.dzirbel.kotify.util.averageOrNull

/**
 * Represents a set of associated [ratings] (by their ID) which can be queried for the [averagePercent] rating of the
 * collection.
 *
 * TODO used by RatingHistogram as well - not always treated as just an average
 */
class AverageRating(val ratings: Iterable<Rating?>) {
    val averagePercent by lazy {
        ratings.averageOrNull { it.ratingPercent }
    }

    val numRatings by lazy {
        ratings.count { it != null }
    }

    companion object {
        val empty = AverageRating(emptyList())
    }
}
