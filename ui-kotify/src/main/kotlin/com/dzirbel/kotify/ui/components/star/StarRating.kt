package com.dzirbel.kotify.ui.components.star

import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.onClick
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.repository.rating.Rating
import com.dzirbel.kotify.ui.components.liveRelativeDateText
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.applyIf
import com.dzirbel.kotify.ui.util.instrumentation.instrument

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
    val dropdownVisible = remember { mutableStateOf(false) }
    val maxRating = rating?.maxRating ?: Rating.DEFAULT_MAX_RATING

    Box(
        modifier = modifier
            .instrument()
            .applyIf(rating != null) {
                onClick(matcher = PointerMatcher.mouse(PointerButton.Secondary)) {
                    dropdownVisible.value = true
                }
            },
    ) {
        StarRow(
            getStarRating = { rating?.rating },
            stars = maxRating,
            enabled = enabled,
            starSize = starSize,
            onClickStar = { star -> onRate(Rating(rating = star, maxRating = maxRating)) },
        )

        if (rating != null) {
            DropdownMenu(
                expanded = dropdownVisible.value,
                onDismissRequest = { dropdownVisible.value = false },
            ) {
                DropdownMenuItem(onClick = {}) {
                    Text("Rated ${liveRelativeDateText(rating.rateTime.toEpochMilli())}")
                }

                DropdownMenuItem(
                    onClick = {
                        onRate(null)
                        dropdownVisible.value = false
                    },
                ) {
                    Text("Clear rating")
                }
            }
        }
    }
}
