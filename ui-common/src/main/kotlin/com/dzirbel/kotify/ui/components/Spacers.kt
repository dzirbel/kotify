package com.dzirbel.kotify.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp

/**
 * A spacer which takes up [width] horizontal space.
 *
 * TODO replace with standard Spacer()
 */
@Composable
fun HorizontalSpacer(width: Dp) {
    Layout(
        content = {},
        measurePolicy = { _, _ ->
            layout(width = width.roundToPx(), height = 0) {}
        },
    )
}

/**
 * A spacer which takes up [height] vertical space.
 *
 * TODO replace with standard Spacer()
 */
@Composable
fun VerticalSpacer(height: Dp) {
    Layout(
        content = {},
        measurePolicy = { _, _ ->
            layout(width = 0, height = height.roundToPx()) {}
        },
    )
}
