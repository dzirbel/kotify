package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.mouseClickable
import androidx.compose.material.ContentAlpha
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.repository.Rating
import com.dzirbel.kotify.ui.theme.Colors
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
    val dropdownVisible = remember { mutableStateOf(false) }

    Row(
        modifier = if (rating != null) {
            modifier.mouseClickable {
                if (buttons.isSecondaryPressed) {
                    dropdownVisible.value = true
                }
            }
        } else {
            modifier
        }
    ) {
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
                    }
                ) {
                    Text("Clear rating")
                }
            }
        }

        val maxRating = rating?.maxRating ?: Rating.DEFAULT_MAX_RATING
        val ratedColor = Colors.current.star
        val unratedColor = Colors.current.text.copy(alpha = ContentAlpha.disabled)

        repeat(maxRating) { star ->
            val rated = rating != null && rating.rating > star

            IconButton(
                enabled = enabled,
                onClick = { onRate(Rating(rating = star + 1, maxRating = maxRating)) },
            ) {
                Icon(
                    modifier = Modifier.size(starSize),
                    imageVector = if (rated) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = "Rate ${star + 1} stars",
                    tint = if (rated) ratedColor else unratedColor,
                )
            }
        }
    }
}
