package com.dzirbel.kotify.ui.components.star

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors

/**
 * Base Composable which renders a row of stars displaying a rating.
 *
 * @param getStarRating retrieves the current star rating, from 1 to [stars]. Passed as a lambda to defer reads to the
 *  draw phase
 * @param stars the maximum number of stars to be shown
 * @param modifier Modifier applied to the row
 * @param enabled whether the user can interact with the rating; if true, a hover state is displayed and [onClickStar]
 *  is invoked on clicks
 * @param onClickStar callback invoked when a star is clicked, with its 1-based index
 * @param starSpacing optional spacing between the stars in the row
 * @param starSize size of each star
 * @param starColor color of the stars when "filled in"
 * @param backgroundColor background color of the stars when not "filled in"
 * @param starPainter [Painter] used to render each star icon
 */
@Composable
fun StarRow(
    getStarRating: () -> Number?,
    stars: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClickStar: ((Int) -> Unit)? = null,
    starSpacing: Dp = 0.dp,
    starSize: Dp = Dimens.iconSmall,
    starColor: Color = LocalColors.current.star,
    backgroundColor: Color = LocalColors.current.text.copy(alpha = ContentAlpha.disabled),
    starPainter: Painter = rememberVectorPainter(Icons.Filled.Star),
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(starSpacing),
    ) {
        // state tracking the set of stars which are currently being hovered (generally only one, but could be two if
        // the enter interaction for one star is emitted before the exit for the previous one)
        val hoveringStars = remember { mutableStateListOf<Int>() }

        // state tracking the star index currently considered hovered, or null if none are hovered
        val hoveredStar = remember {
            derivedStateOf { hoveringStars.maxOrNull() }
        }

        repeat(stars) { star ->
            val hoverInteractionSource = remember { MutableInteractionSource() }
            LaunchedEffect(hoverInteractionSource) {
                hoverInteractionSource.interactions.collect { interaction ->
                    when (interaction) {
                        is HoverInteraction.Enter -> hoveringStars.add(star)
                        is HoverInteraction.Exit -> hoveringStars.remove(star)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(starSize)
                    .drawWithCache {
                        @Suppress("MagicNumber")
                        val addingHoverColor = lerp(backgroundColor, starColor, 0.7f)

                        @Suppress("MagicNumber")
                        val removingHoverColor = lerp(backgroundColor, starColor, 0.4f)

                        val starFilter = ColorFilter.tint(starColor)
                        val backgroundFilter = ColorFilter.tint(backgroundColor)
                        val addingHoverFilter = ColorFilter.tint(addingHoverColor)
                        val removingHoverFilter = ColorFilter.tint(removingHoverColor)

                        onDrawWithContent {
                            val hoveredStarValue = hoveredStar.value
                            val hoveringMore = hoveredStarValue != null && hoveredStarValue >= star
                            val hoveringLess = hoveredStarValue != null && hoveredStarValue < star

                            val bg = if (hoveringMore) addingHoverFilter else backgroundFilter
                            val fg = if (hoveringLess) removingHoverFilter else starFilter

                            val rating = getStarRating() ?: 0

                            if (rating.toInt() > star) { // star is full
                                with(starPainter) { draw(size, colorFilter = fg) }
                            } else {
                                if (rating.toFloat() > star) { // star is partially filled
                                    with(starPainter) { draw(size, colorFilter = bg) }
                                    clipRect(right = (rating.toFloat() - rating.toInt()) * size.width) {
                                        with(starPainter) { draw(size, colorFilter = fg) }
                                    }
                                } else { // star is empty
                                    with(starPainter) { draw(size, colorFilter = bg) }
                                }
                            }
                        }
                    }
                    .hoverable(enabled = enabled, interactionSource = hoverInteractionSource)
                    .onClick(enabled = enabled) { onClickStar?.invoke(star + 1) },
            )
        }
    }
}
