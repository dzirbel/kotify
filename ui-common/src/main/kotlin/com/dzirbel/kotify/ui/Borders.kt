package com.dzirbel.kotify.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.ui.components.panel.PanelDirection
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.KotifyColors

@Composable
@Stable
@Suppress("ModifierComposable") // composable to allow access to current colors
fun Modifier.border(
    direction: PanelDirection,
    width: Dp = Dimens.divider,
    padWidth: Boolean = true,
    color: Color = KotifyColors.current.divider,
): Modifier {
    return if (color == Color.Transparent) {
        // do not add unnecessary graphics layer when border color is transparent
        if (padWidth) {
            borderPadding(direction = direction, width = width)
        } else {
            this
        }
    } else {
        border(direction = direction, brush = SolidColor(color), width = width, padWidth = padWidth)
    }
}

@Stable
fun Modifier.border(direction: PanelDirection, stroke: BorderStroke, padWidth: Boolean = true): Modifier {
    return border(direction = direction, brush = stroke.brush, width = stroke.width, padWidth = padWidth)
}

@Stable
fun Modifier.border(
    direction: PanelDirection,
    brush: Brush,
    width: Dp = Dimens.divider,
    padWidth: Boolean = true,
): Modifier {
    val paddingModifier = if (padWidth) {
        borderPadding(direction = direction, width = width)
    } else {
        this
    }

    return paddingModifier
        .drawWithContent {
            val widthPx = width.toPx()
            val start: Offset = when (direction) {
                PanelDirection.LEFT -> Offset(widthPx / 2, 0f)
                PanelDirection.RIGHT -> Offset(size.width - widthPx / 2, 0f)
                PanelDirection.TOP -> Offset(0f, widthPx / 2)
                PanelDirection.BOTTOM -> Offset(0f, size.height - widthPx / 2)
            }

            val end: Offset = when (direction) {
                PanelDirection.LEFT -> Offset(widthPx / 2, size.height)
                PanelDirection.RIGHT -> Offset(size.width - widthPx / 2, size.height)
                PanelDirection.TOP -> Offset(size.width, widthPx / 2)
                PanelDirection.BOTTOM -> Offset(size.width, size.height - widthPx / 2)
            }

            drawContent()
            drawLine(brush = brush, start = start, end = end, strokeWidth = widthPx)
        }
}

@Stable
private fun Modifier.borderPadding(direction: PanelDirection, width: Dp = Dimens.divider): Modifier {
    return when (direction) {
        PanelDirection.LEFT -> padding(end = width)
        PanelDirection.RIGHT -> padding(start = width)
        PanelDirection.TOP -> padding(top = width)
        PanelDirection.BOTTOM -> padding(bottom = width)
    }
}
