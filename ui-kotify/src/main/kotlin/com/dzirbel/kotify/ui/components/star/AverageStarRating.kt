package com.dzirbel.kotify.ui.components.star

import androidx.compose.foundation.layout.Row
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.repository.Rating
import com.dzirbel.kotify.ui.components.HorizontalSpacer
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.util.instrumentation.instrument
import com.dzirbel.kotify.util.averageAndCountOrNull
import kotlinx.collections.immutable.ImmutableList

@Composable
fun AverageStarRating(
    ratings: ImmutableList<Rating?>?,
    maxRating: Int = Rating.DEFAULT_MAX_AVERAGE_RATING,
    starSize: Dp = Dimens.iconSmall,
) {
    val (averageRating, totalRatings) = remember(ratings) {
        ratings?.averageAndCountOrNull { it.ratingRelativeToMax(maxRating = maxRating) }
            ?: Pair(null, 0)
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.instrument()) {
        StarRow(
            getStarRating = { averageRating },
            stars = maxRating,
            starSize = starSize,
            enabled = false,
        )

        if (averageRating != null) {
            HorizontalSpacer(width = Dimens.space1)

            Text(
                text = "%.1f (%d)".format(averageRating, totalRatings),
                color = LocalColors.current.text.copy(alpha = ContentAlpha.medium),
            )
        }
    }
}
