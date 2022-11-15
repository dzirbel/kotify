package com.dzirbel.kotify.ui.components.star

import androidx.compose.foundation.background
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
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.repository.Rating
import com.dzirbel.kotify.ui.components.HorizontalDivider
import com.dzirbel.kotify.ui.components.hoverState
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import kotlin.math.max
import kotlin.math.roundToInt

private const val BAR_WIDTH_PERCENT = 0.85f
private val BAR_WIDTH_DEFAULT = 28.dp
private val BAR_HEIGHT_DEFAULT = 150.dp

@Composable
fun RatingHistogram(
    ratings: List<State<Rating?>>,
    maxRating: Int = Rating.DEFAULT_MAX_RATING,
    barWidth: Dp = BAR_WIDTH_DEFAULT,
    barHeight: Dp = BAR_HEIGHT_DEFAULT,
) {
    val ratingCounts = MutableList(maxRating + 1) { 0 }
    var maxRatingCount = 0
    ratings.forEach { ratingState ->
        ratingState.value?.let { rating ->
            val ratingRelative = rating.ratingRelativeToMax(maxRating = maxRating).roundToInt()
            maxRatingCount = max(maxRatingCount, ++ratingCounts[ratingRelative])
        }
    }

    Row {
        repeat(maxRating) { rating ->
            val columnHover = remember { mutableStateOf(false) }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.hoverState(columnHover)) {
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
                            .background(LocalColors.current.star),
                    ) {
                        if (columnHover.value && numRatings > 0) {
                            Text(
                                text = numRatings.toString(),
                                style = MaterialTheme.typography.overline,
                                color = LocalColors.current.textOnSurface,
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
