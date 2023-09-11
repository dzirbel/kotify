package com.dzirbel.kotify.ui.components.star

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.repository.rating.AverageRating
import com.dzirbel.kotify.repository.rating.Rating
import com.dzirbel.kotify.ui.components.HorizontalDivider
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.KotifyColors
import com.dzirbel.kotify.ui.util.instrumentation.instrument
import kotlin.math.max
import kotlin.math.roundToInt

private const val BAR_WIDTH_PERCENT = 0.85f
private val BAR_WIDTH_DEFAULT = 28.dp
private val BAR_HEIGHT_DEFAULT = 150.dp

@Composable
fun RatingHistogram(
    ratings: AverageRating,
    maxRating: Int = Rating.DEFAULT_MAX_RATING,
    barWidth: Dp = BAR_WIDTH_DEFAULT,
    barHeight: Dp = BAR_HEIGHT_DEFAULT,
) {
    val ratingCounts = MutableList(maxRating + 1) { 0 }
    var maxRatingCount = 0
    ratings.ratings.forEach { rating ->
        if (rating != null) {
            val ratingRelative = rating.ratingRelativeToMax(maxRating = maxRating).roundToInt()
            maxRatingCount = max(maxRatingCount, ++ratingCounts[ratingRelative])
        }
    }

    Row(Modifier.instrument()) {
        repeat(maxRating) { rating ->
            val hoverInteractionSource = remember(rating) { MutableInteractionSource() }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.hoverable(hoverInteractionSource),
            ) {
                val numRatings = ratingCounts[rating + 1]
                Column(
                    modifier = Modifier.height(barHeight).width(barWidth),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val height = if (maxRatingCount == 0) 0f else numRatings.toFloat() / maxRatingCount
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(height)
                            .fillMaxWidth(BAR_WIDTH_PERCENT)
                            .background(KotifyColors.current.star),
                    ) {
                        val hovering = hoverInteractionSource.collectIsHoveredAsState().value
                        if (hovering && numRatings > 0) {
                            Text(
                                text = numRatings.toString(),
                                style = MaterialTheme.typography.overline,
                                color = MaterialTheme.colors.onPrimary,
                                modifier = Modifier.align(Alignment.TopCenter),
                                maxLines = 1,
                                overflow = TextOverflow.Visible,
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.width(barWidth))

                Text(
                    text = (rating + 1).toString(),
                    modifier = Modifier.padding(top = Dimens.space1),
                )
            }
        }
    }
}
