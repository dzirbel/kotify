package com.dzirbel.kotify.ui.components.star

import androidx.compose.foundation.layout.Row
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.repository.AverageRating
import com.dzirbel.kotify.repository.Rating
import com.dzirbel.kotify.ui.components.HorizontalSpacer
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.util.instrumentation.instrument

@Composable
fun AverageStarRating(
    averageRating: AverageRating?,
    maxRating: Int = Rating.DEFAULT_MAX_AVERAGE_RATING,
    starSize: Dp = Dimens.iconSmall,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.instrument()) {
        val stars = averageRating?.averagePercent?.let { it * maxRating }
        StarRow(
            getStarRating = { stars },
            stars = maxRating,
            starSize = starSize,
            enabled = false,
        )

        if (stars != null) {
            HorizontalSpacer(width = Dimens.space1)

            Text(
                text = "%.1f (%d)".format(stars, averageRating.numRatings),
                color = LocalColors.current.text.copy(alpha = ContentAlpha.medium),
            )
        }
    }
}
