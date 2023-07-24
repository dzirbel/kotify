package com.dzirbel.kotify.repository.rating

import com.dzirbel.kotify.util.averageOrNull

/**
 * Represents a set of associated [ratings] (by their ID) which can be queried for the [averagePercent] rating of the
 * collection.
 *
 * TODO used by RatingHistogram as well - not always treated as just an average
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
