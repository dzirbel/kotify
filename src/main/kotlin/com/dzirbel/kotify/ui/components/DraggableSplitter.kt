package com.dzirbel.kotify.ui.components

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import java.awt.Cursor

data class SplitterViewParams(
    val dragTargetWidth: Dp = Dimens.space3,
    val lineWidth: Dp = Dimens.divider,
    val lineColor: Color = Color.Unspecified,
)

/**
 * A vertical or horizontal (depending on [orientation]) divider which can be dragged either horizontally or vertically,
 * respectively.
 */
@Composable
fun DraggableSplitter(
    orientation: Orientation,
    resizeEnabled: Boolean = true,
    params: SplitterViewParams = SplitterViewParams(),
    onResize: (delta: Dp) -> Unit,
) {
    val resizing = remember { mutableStateOf(false) }

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
                    if (resizeEnabled) {
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
                                onDragStarted = { resizing.value = true },
                                onDragStopped = { resizing.value = false }
                            )
                            .pointerHoverIcon(
                                PointerIcon(
                                    when (orientation) {
                                        Orientation.Horizontal -> Cursor(Cursor.N_RESIZE_CURSOR)
                                        Orientation.Vertical -> Cursor(Cursor.E_RESIZE_CURSOR)
                                    }
                                )
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
                .background(params.lineColor.takeIf { it.isSpecified } ?: LocalColors.current.dividerColor)
        )
    }
}
