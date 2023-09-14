package com.dzirbel.kotify.ui

import com.dzirbel.kotify.repository.rating.Rating
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.random.asJavaRandom

fun Random.nextGaussian(mean: Number, stddev: Number, min: Number? = null, max: Number? = null): Double {
    return asJavaRandom().nextGaussian(mean.toDouble(), stddev.toDouble())
        .let { if (min != null) it.coerceAtLeast(min.toDouble()) else it }
        .let { if (max != null) it.coerceAtMost(max.toDouble()) else it }
}

fun Random.nextGaussianRating(
    mean: Number = 5.0,
    stddev: Number = 0.5,
    maxRating: Int = Rating.DEFAULT_MAX_RATING,
): Rating {
    return Rating(
        rating = nextGaussian(mean = mean, stddev = stddev, min = 1, max = maxRating).roundToInt(),
        maxRating = maxRating,
    )
}
