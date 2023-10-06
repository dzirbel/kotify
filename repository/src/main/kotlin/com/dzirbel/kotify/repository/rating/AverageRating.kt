package com.dzirbel.kotify.repository.rating

import androidx.compose.runtime.Stable
import com.dzirbel.kotify.util.collections.averageBy

/**
 * Represents a set of associated [ratings] (by their ID) which can be queried for the [averagePercent] rating of the
 * collection.
 *
 * TODO used by RatingHistogram as well - not always treated as just an average
 */
@Stable
data class AverageRating(val ratings: List<Rating?>) {
    val averagePercent by lazy {
        ratings.averageBy { it.ratingPercent }
    }

    val numRatings by lazy {
        ratings.count { it != null }
    }

    companion object {
        val empty = AverageRating(emptyList())
    }
}
