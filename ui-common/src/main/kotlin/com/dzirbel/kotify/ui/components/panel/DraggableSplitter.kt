package com.dzirbel.kotify.ui.components.panel

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import java.awt.Cursor

/**
 * A vertical or horizontal (depending on [orientation]) divider which can be dragged either horizontally or vertically,
 * respectively.
 */
@Composable
fun DraggableSplitter(orientation: Orientation, dragTargetSize: Dp, onResize: (delta: Dp) -> Unit) {
    val density = LocalDensity.current
    Box(
        Modifier
            .run {
                when (orientation) {
                    Orientation.Vertical -> width(dragTargetSize).fillMaxHeight()
                    Orientation.Horizontal -> height(dragTargetSize).fillMaxWidth()
                }
            }
            .draggable(
                state = rememberDraggableState { delta ->
                    with(density) { onResize(delta.toDp()) }
                },
                orientation = when (orientation) {
                    Orientation.Horizontal -> Orientation.Vertical
                    Orientation.Vertical -> Orientation.Horizontal
                },
                startDragImmediately = true,
            )
            .pointerHoverIcon(
                PointerIcon(
                    when (orientation) {
                        Orientation.Horizontal -> Cursor(Cursor.N_RESIZE_CURSOR)
                        Orientation.Vertical -> Cursor(Cursor.E_RESIZE_CURSOR)
                    },
                ),
            ),
    )
}
