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
import com.dzirbel.kotify.ui.theme.Colors
import com.dzirbel.kotify.ui.theme.Dimens

/**
 * A generic star rating component, represented as a row of clickable stars.
 *
 * @param rating the current rating, i.e the number of stars currently given. If <1 then no stars will be highlighted,
 *               if greater than [stars] then all will be highlighted, but the typical range is [1, stars]
 * @param stars the maximum number of stars to show
 * @param modifier [Modifier] applied to the entire component
 * @param ratedTimestamp optional timestamp when the rating was given
 * @param enabled whether rating buttons are enabled
 * @param starSize size of each star
 * @param onRate callback invoked when a star is clicked
 * @param onClearRating optional callback invoked when the rating is cleared; if null the option is not given
 */
@Composable
fun StarRating(
    rating: Int?,
    stars: Int,
    modifier: Modifier,
    ratedTimestamp: Long? = null,
    enabled: Boolean = true,
    starSize: Dp = Dimens.iconSmall,
    onRate: (Int) -> Unit,
    onClearRating: (() -> Unit)? = null,
) {
    val dropdownNotEmpty = ratedTimestamp != null || onClearRating != null
    val dropdownVisible = remember { mutableStateOf(false) }

    Row(
        modifier = if (dropdownNotEmpty) {
            modifier.mouseClickable {
                if (buttons.isSecondaryPressed) {
                    dropdownVisible.value = true
                }
            }
        } else {
            modifier
        }
    ) {
        DropdownMenu(
            expanded = dropdownVisible.value,
            onDismissRequest = { dropdownVisible.value = false },
        ) {
            if (ratedTimestamp != null) {
                DropdownMenuItem(onClick = {}) {
                    Text("Rated ${liveRelativeDateText(ratedTimestamp)}")
                }
            }

            if (onClearRating != null) {
                DropdownMenuItem(
                    onClick = {
                        onClearRating()
                        dropdownVisible.value = false
                    }
                ) {
                    Text("Clear rating")
                }
            }
        }

        val ratedColor = Colors.current.star
        val unratedColor = Colors.current.text.copy(alpha = ContentAlpha.disabled)

        repeat(stars) { star ->
            val rated = rating != null && rating > star

            IconButton(
                enabled = enabled,
                onClick = { onRate(star + 1) },
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
