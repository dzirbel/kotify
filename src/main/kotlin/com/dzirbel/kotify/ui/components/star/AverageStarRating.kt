package com.dzirbel.kotify.ui.components.star

import androidx.compose.foundation.layout.Row
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.repository.Rating
import com.dzirbel.kotify.ui.components.HorizontalSpacer
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.util.averageOrNull
import kotlin.math.floor

@Composable
fun AverageStarRating(
    ratings: Iterable<Rating?>?,
    maxRating: Int = Rating.DEFAULT_MAX_AVERAGE_RATING,
    starSize: Dp = Dimens.iconSmall,
) {
    val averageRating = remember(ratings) {
        ratings?.averageOrNull { it.ratingRelativeToMax(maxRating = maxRating) }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (averageRating == null) {
            repeat(maxRating) {
                StarIcon(starSize = starSize, filled = false)
            }
        } else {
            repeat(maxRating) { star ->
                val fullyRated = floor(averageRating) > star
                val partiallyRated = !fullyRated && averageRating > star

                when {
                    fullyRated -> StarIcon(starSize = starSize, filled = true)

                    partiallyRated ->
                        StarIcon(starSize = starSize, filledPercent = averageRating - floor(averageRating))

                    else -> StarIcon(starSize = starSize, filled = false)
                }
            }

            HorizontalSpacer(width = Dimens.space1)

            Text(
                text = "%.1f".format(averageRating),
                color = LocalColors.current.text.copy(alpha = ContentAlpha.medium),
            )
        }
    }
}
