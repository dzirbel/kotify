package com.dominiczirbel.ui

import androidx.compose.desktop.LocalAppWindow
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.awt.Cursor

interface SplitterState {
    var isResizing: Boolean
    var isResizeEnabled: Boolean
}

@Composable
fun DraggableSplitter(
    orientation: Orientation,
    splitterState: SplitterState,
    @Suppress("MagicNumber") dragTargetWidth: Dp = 8.dp,
    lineWidth: Dp = 1.dp,
    lineColor: Color = Color.Black,
    onResize: (delta: Dp) -> Unit,
) = Box {
    val density = LocalDensity.current
    Box(
        Modifier
            .run {
                when (orientation) {
                    Orientation.Vertical -> width(dragTargetWidth).fillMaxHeight()
                    Orientation.Horizontal -> height(dragTargetWidth).fillMaxWidth()
                }
            }
            .run {
                if (splitterState.isResizeEnabled) {
                    this.draggable(
                        state = rememberDraggableState {
                            with(density) {
                                onResize(it.toDp())
                            }
                        },
                        orientation = when (orientation) {
                            Orientation.Horizontal -> Orientation.Vertical
                            Orientation.Vertical -> Orientation.Horizontal
                        },
                        startDragImmediately = true,
                        onDragStarted = { splitterState.isResizing = true },
                        onDragStopped = { splitterState.isResizing = false }
                    ).draggingCursor(orientation = orientation)
                } else {
                    this
                }
            }
    )

    Box(
        Modifier
            .run {
                when (orientation) {
                    Orientation.Vertical -> width(lineWidth).fillMaxHeight()
                    Orientation.Horizontal -> height(lineWidth).fillMaxWidth()
                }
            }
            .background(lineColor)
    )
}

private fun Modifier.draggingCursor(orientation: Orientation): Modifier {
    return composed {
        var isHover by remember { mutableStateOf(false) }

        LocalAppWindow.current.window.cursor = if (isHover) {
            when (orientation) {
                Orientation.Horizontal -> Cursor(Cursor.N_RESIZE_CURSOR)
                Orientation.Vertical -> Cursor(Cursor.E_RESIZE_CURSOR)
            }
        } else {
            Cursor.getDefaultCursor()
        }

        pointerMoveFilter(
            onEnter = { true.also { isHover = true } },
            onExit = { true.also { isHover = false } }
        )
    }
}
