package com.dzirbel.kotify.ui.components.star

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.repository.rating.AverageRating
import com.dzirbel.kotify.repository.rating.Rating
import com.dzirbel.kotify.ui.theme.Dimens
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
            Spacer(Modifier.width(Dimens.space1))

            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                Text(text = "%.1f (%d)".format(stars, averageRating.numRatings))
            }
        }
    }
}
