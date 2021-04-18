package com.dzirbel.kotify.ui.common

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.ui.theme.Colors
import com.dzirbel.kotify.ui.theme.Dimens
import java.awt.Cursor

interface SplitterState {
    var isResizing: Boolean
    var isResizeEnabled: Boolean
}

data class SplitterViewParams(
    val dragTargetWidth: Dp = Dimens.space3,
    val lineWidth: Dp = Dimens.divider,
    val lineColor: Color = Colors.current.dividerColor
)

@Composable
fun DraggableSplitter(
    orientation: Orientation,
    splitterState: SplitterState,
    params: SplitterViewParams = SplitterViewParams(),
    onResize: (delta: Dp) -> Unit
) {
    Box {
        val density = LocalDensity.current
        Box(
            Modifier
                .run {
                    when (orientation) {
                        Orientation.Vertical -> width(params.dragTargetWidth).fillMaxHeight()
                        Orientation.Horizontal -> height(params.dragTargetWidth).fillMaxWidth()
                    }
                }
                .run {
                    if (splitterState.isResizeEnabled) {
                        this
                            .draggable(
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
                            )
                            .hoverCursor(
                                when (orientation) {
                                    Orientation.Horizontal -> Cursor(Cursor.N_RESIZE_CURSOR)
                                    Orientation.Vertical -> Cursor(Cursor.E_RESIZE_CURSOR)
                                }
                            )
                    } else {
                        this
                    }
                }
        )

        Box(
            Modifier
                .run {
                    when (orientation) {
                        Orientation.Vertical -> width(params.lineWidth).fillMaxHeight()
                        Orientation.Horizontal -> height(params.lineWidth).fillMaxWidth()
                    }
                }
                .background(params.lineColor)
        )
    }
}
