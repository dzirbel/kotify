package com.dzirbel.kotify.ui.contextmenu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import kotlin.math.roundToInt

/**
 * Returns a [PopupPositionProvider] which for nested context menu dropdowns.
 *
 * This is an altered version of [androidx.compose.ui.window.rememberComponentRectPositionProvider] which adds logic to
 * ensure that the dropdown is always visible within the window, with the given [windowMargin].
 */
@Composable
fun rememberNestedDropdownPositionProvider(
    windowMargin: Dp,
    anchor: Alignment = Alignment.TopEnd,
    alignment: Alignment = Alignment.BottomEnd,
    offset: DpOffset = DpOffset.Zero,
): PopupPositionProvider {
    val offsetPx = if (offset == DpOffset.Zero) {
        Offset.Zero
    } else {
        with(LocalDensity.current) { Offset(offset.x.toPx(), offset.y.toPx()) }
    }

    val windowMarginPx = with(LocalDensity.current) { windowMargin.roundToPx() }

    return remember(anchor, alignment, offsetPx, windowMarginPx) {
        NestedDropdownPositionProvider(
            anchor = anchor,
            alignment = alignment,
            offsetPx = offsetPx,
            windowMarginPx = windowMarginPx,
        )
    }
}

private class NestedDropdownPositionProvider(
    private val anchor: Alignment,
    private val alignment: Alignment,
    private val offsetPx: Offset,
    private val windowMarginPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val anchorPoint = anchor.align(
            size = IntSize.Zero,
            space = anchorBounds.size,
            layoutDirection = layoutDirection,
        )

        val tooltipArea = IntRect(
            offset = IntOffset(
                x = anchorBounds.left + anchorPoint.x - popupContentSize.width,
                y = anchorBounds.top + anchorPoint.y - popupContentSize.height,
            ),
            size = IntSize(
                width = popupContentSize.width * 2,
                height = popupContentSize.height * 2,
            ),
        )

        val position = alignment.align(popupContentSize, tooltipArea.size, layoutDirection)

        var x = tooltipArea.left + position.x + offsetPx.x
        var y = tooltipArea.top + position.y + offsetPx.y
        if (x + popupContentSize.width > windowSize.width - windowMarginPx) {
            x -= popupContentSize.width + anchorBounds.width
        }
        if (y + popupContentSize.height > windowSize.height - windowMarginPx) {
            y -= popupContentSize.height - anchorBounds.height
        }
        x = x.coerceAtLeast(windowMarginPx.toFloat())
        y = y.coerceAtLeast(windowMarginPx.toFloat())

        return IntOffset(x.roundToInt(), y.roundToInt())
    }
}
