package com.dzirbel.kotify.ui.components.star

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.dzirbel.contextmenu.ContextMenuParams
import com.dzirbel.contextmenu.CustomContentContextMenuItem
import com.dzirbel.contextmenu.MaterialContextMenuItem
import com.dzirbel.kotify.repository.rating.Rating
import com.dzirbel.kotify.ui.components.liveRelativeTime
import com.dzirbel.kotify.ui.theme.Dimens

/**
 * A generic star rating component, represented as a row of clickable stars.
 *
 * @param rating the current [Rating], i.e the number of stars currently given and the maximum number of stars
 * @param modifier applied to the entire component
 * @param enabled whether rating buttons are enabled
 * @param starSize size of each star
 * @param onRate callback invoked when a star is clicked or the rating is cleared
 */
@Composable
fun StarRating(
    rating: Rating?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    starSize: Dp = Dimens.iconSmall,
    onRate: (Rating?) -> Unit,
) {
    ContextMenuArea(
        enabled = rating != null,
        items = {
            if (rating != null) {
                listOf(
                    object : CustomContentContextMenuItem(onClick = {}) {
                        override val clickable = false

                        @Composable
                        override fun Content(onDismissRequest: () -> Unit, params: ContextMenuParams) {
                            val relativeTime = liveRelativeTime(rating.rateTime.toEpochMilli())
                            Text("Rated ${relativeTime.formatLong()}")
                        }
                    },
                    MaterialContextMenuItem(
                        label = "Clear rating",
                        onClick = { onRate(null) },
                    ),
                )
            } else {
                emptyList()
            }
        },
    ) {
        val maxRating = rating?.maxRating ?: Rating.DEFAULT_MAX_RATING
        StarRow(
            modifier = modifier,
            getStarRating = { rating?.rating },
            stars = maxRating,
            enabled = enabled,
            starSize = starSize,
            onClickStar = { star -> onRate(Rating(rating = star, maxRating = maxRating)) },
        )
    }
}
