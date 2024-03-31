package com.dzirbel.kotify.ui.components.star

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.KotifyColors
import com.dzirbel.kotify.ui.theme.StarColors

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
 * @param starColors colors used to render the stars
 * @param starSpacing optional spacing between the stars in the row
 * @param starSize size of each star
 * @param starOutlineWidth width of the outline around each star, if any
 * @param starPainter [Painter] used to render each star icon
 */
@Composable
fun StarRow(
    getStarRating: () -> Number?,
    stars: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClickStar: ((Int) -> Unit)? = null,
    starColors: StarColors = KotifyColors.current.star,
    starSpacing: Dp = 0.dp,
    starSize: Dp = Dimens.iconSmall,
    starOutlineWidth: Dp = 2.dp,
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
                        val outlineWidthPx = starOutlineWidth.toPx()

                        val starColorFilter = ColorFilter.tint(starColors.foreground)
                        val backgroundColorFilter = ColorFilter.tint(starColors.background)
                        val starOutlineColorFilter = starColors.outline?.let { ColorFilter.tint(it) }

                        onDrawWithContent {
                            fun draw(color: ColorFilter, alpha: Float = 1f) {
                                val outline = starOutlineColorFilter.takeIf { color !== backgroundColorFilter }
                                if (outline == null) {
                                    with(starPainter) { draw(size = size, colorFilter = color, alpha = alpha) }
                                } else {
                                    with(starPainter) { draw(size = size, colorFilter = outline, alpha = alpha) }
                                    translate(outlineWidthPx, outlineWidthPx) {
                                        with(starPainter) {
                                            draw(
                                                size = Size(
                                                    width = size.width - outlineWidthPx * 2,
                                                    height = size.height - outlineWidthPx * 2,
                                                ),
                                                colorFilter = color,
                                                alpha = alpha,
                                            )
                                        }
                                    }
                                }
                            }

                            val hoveredStarValue = hoveredStar.value
                            val hoveringMore = hoveredStarValue != null && hoveredStarValue >= star
                            val hoveringLess = hoveredStarValue != null && hoveredStarValue < star

                            val rating = getStarRating() ?: 0

                            when {
                                // star is fully rated
                                rating.toInt() > star ->
                                    if (hoveringLess) {
                                        draw(starColorFilter, alpha = starColors.removingAlpha)
                                    } else {
                                        draw(starColorFilter)
                                    }

                                // star is partially rated
                                rating.toFloat() > star -> {
                                    if (hoveringMore) {
                                        draw(starColorFilter, alpha = starColors.addingAlpha)
                                    } else {
                                        draw(backgroundColorFilter)
                                    }

                                    clipRect(right = (rating.toFloat() - rating.toInt()) * size.width) {
                                        if (hoveringLess) {
                                            draw(starColorFilter, alpha = starColors.removingAlpha)
                                        } else {
                                            draw(starColorFilter)
                                        }
                                    }
                                }

                                else ->
                                    if (hoveringMore) {
                                        draw(starColorFilter, alpha = starColors.addingAlpha)
                                    } else {
                                        draw(backgroundColorFilter)
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
