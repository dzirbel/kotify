package com.dominiczirbel.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

enum class PanelDirection { LEFT, RIGHT, TOP, BOTTOM }

// TODO support min/maxes as percentages
class PanelState(
    initialSize: Dp,
    private val minSize: Dp? = null,
    private val maxSize: Dp? = null,
    val minContentSize: Dp? = null,
    resizeEnabled: Boolean = true
) : SplitterState {
    private var _size by mutableStateOf(initialSize)

    init {
        require(maxSize == null || minContentSize == null) {
            "cannot set both a panel max size and a content min size: this could lead to cases where neither the " +
                "panel nor the content can fill the view"
        }
    }

    var size
        get() = _size
        set(value) {
            _size = value.coerceAtMostNullable(maxSize).coerceAtLeastNullable(minSize)
        }

    override var isResizing by mutableStateOf(false)
    override var isResizeEnabled by mutableStateOf(resizeEnabled)

    private fun <T : Comparable<T>> T.coerceAtMostNullable(maximumValue: T?): T {
        return maximumValue?.let { coerceAtMost(it) } ?: this
    }

    private fun <T : Comparable<T>> T.coerceAtLeastNullable(minimumValue: T?): T {
        return minimumValue?.let { coerceAtLeast(it) } ?: this
    }
}

@Composable
fun SidePanel(
    direction: PanelDirection,
    state: PanelState,
    modifier: Modifier = Modifier.fillMaxSize(),
    panelContent: @Composable () -> Unit,
    mainContent: @Composable () -> Unit
) {
    val splitterOrientation = when (direction) {
        PanelDirection.LEFT, PanelDirection.RIGHT -> Orientation.Vertical
        PanelDirection.TOP, PanelDirection.BOTTOM -> Orientation.Horizontal
    }
    val density = LocalDensity.current

    Layout(
        modifier = modifier,
        content = {
            Box { panelContent() }
            Box { mainContent() }
            DraggableSplitter(
                orientation = splitterOrientation,
                splitterState = state,
                onResize = { delta ->
                    // invert delta for right/bottom dragging
                    val adjustedDelta = when (direction) {
                        PanelDirection.LEFT, PanelDirection.TOP -> delta
                        PanelDirection.RIGHT, PanelDirection.BOTTOM -> -delta
                    }

                    state.size = state.size + adjustedDelta
                }
            )
        },
        measureBlock = { measurables, constraints ->
            @Suppress("MagicNumber")
            require(measurables.size == 3)

            val minContentSize = state.minContentSize?.let { with(density) { it.roundToPx() } } ?: 0
            val panelSizePx = with(density) { state.size.roundToPx() }
                .coerceAtLeast(0)
                .coerceAtMost(
                    when (splitterOrientation) {
                        Orientation.Horizontal -> constraints.maxHeight - minContentSize
                        Orientation.Vertical -> constraints.maxWidth - minContentSize
                    }
                )
            val panelPlaceable = measurables[0].measure(
                when (splitterOrientation) {
                    Orientation.Horizontal -> constraints.copy(minHeight = panelSizePx, maxHeight = panelSizePx)
                    Orientation.Vertical -> constraints.copy(minWidth = panelSizePx, maxWidth = panelSizePx)
                }
            )

            val contentSize = when (splitterOrientation) {
                Orientation.Horizontal -> constraints.maxHeight - panelPlaceable.height
                Orientation.Vertical -> constraints.maxWidth - panelPlaceable.width
            }
            val contentPlaceable = measurables[1].measure(
                when (splitterOrientation) {
                    Orientation.Horizontal ->
                        Constraints(
                            minWidth = constraints.maxWidth,
                            maxWidth = constraints.maxWidth,
                            minHeight = contentSize,
                            maxHeight = contentSize
                        )
                    Orientation.Vertical ->
                        Constraints(
                            minWidth = contentSize,
                            maxWidth = contentSize,
                            minHeight = constraints.maxHeight,
                            maxHeight = constraints.maxHeight
                        )
                }
            )

            val splitterPlaceable = measurables[2].measure(constraints)

            val (firstPlaceable, secondPlaceable) = when (direction) {
                PanelDirection.LEFT, PanelDirection.TOP -> Pair(panelPlaceable, contentPlaceable)
                PanelDirection.RIGHT, PanelDirection.BOTTOM -> Pair(contentPlaceable, panelPlaceable)
            }

            layout(constraints.maxWidth, constraints.maxHeight) {
                firstPlaceable.place(0, 0)
                when (splitterOrientation) {
                    Orientation.Horizontal -> {
                        secondPlaceable.place(0, firstPlaceable.height)
                        splitterPlaceable.place(0, firstPlaceable.height)
                    }
                    Orientation.Vertical -> {
                        secondPlaceable.place(firstPlaceable.width, 0)
                        splitterPlaceable.place(firstPlaceable.width, 0)
                    }
                }
            }
        }
    )
}
