package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.ui.theme.Colors
import com.dzirbel.kotify.util.coerceAtLeastNullable
import com.dzirbel.kotify.util.coerceAtMostNullable

enum class PanelDirection { LEFT, RIGHT, TOP, BOTTOM }

sealed class FixedOrPercent {
    abstract fun measure(total: Dp): Dp

    data class Fixed(val value: Dp) : FixedOrPercent() {
        override fun measure(total: Dp) = value
    }

    data class Percent(val value: Float) : FixedOrPercent() {
        override fun measure(total: Dp) = total * value
    }
}

data class PanelParams(
    val initialSize: FixedOrPercent,

    val minPanelSizeDp: Dp? = null,
    val minPanelSizePercent: Float? = null,
    val maxPanelSizeDp: Dp? = null,
    val maxPanelSizePercent: Float? = null,

    val minContentSizeDp: Dp? = null,
    val minContentSizePercent: Float? = null,
    val maxContentSizeDp: Dp? = null,
    val maxContentSizePercent: Float? = null,
) {
    init {
        val hasMinPanelSize = minPanelSizeDp != null || minPanelSizePercent != null
        val hasMaxPanelSize = maxPanelSizeDp != null || maxPanelSizePercent != null
        val hasMinContentSize = minContentSizeDp != null || minContentSizePercent != null
        val hasMaxContentSize = maxContentSizeDp != null || maxContentSizePercent != null

        require(!(hasMinPanelSize && hasMaxContentSize)) {
            "cannot set both a minimum panel size and maximum content size: this could lead to cases where neither " +
                "the panel nor content can fill the view"
        }

        require(!(hasMaxPanelSize && hasMinContentSize)) {
            "cannot set both a maximum panel size and minimum content size: this could lead to cases where neither " +
                "the panel nor content can fill the view"
        }
    }

    /**
     * Determines the minimum panel size with the given [total] size, or zero if there is no minimum size.
     */
    fun minPanelSize(total: Dp): Dp {
        return listOfNotNull(
            minPanelSizeDp,
            minPanelSizePercent?.let { total * it },
            maxContentSizeDp?.let { total - it },
            maxContentSizePercent?.let { total - total * it },
        ).minOrNull() ?: 0.dp
    }

    /**
     * Determines the minimum panel size with the given [total] size, or [total] if there is no maximum size.
     */
    fun maxPanelSize(total: Dp): Dp {
        return listOfNotNull(
            maxPanelSizeDp,
            maxPanelSizePercent?.let { total * it },
            minContentSizeDp?.let { total - it },
            minContentSizePercent?.let { total - total * it },
        ).maxOrNull() ?: total
    }
}

@Composable
fun SidePanel(
    direction: PanelDirection,
    params: PanelParams,
    modifier: Modifier = Modifier.fillMaxSize(),
    panelEnabled: Boolean = true,
    panelModifier: Modifier = Modifier
        .fillMaxSize()
        .background(Colors.current.surface2),
    contentModifier: Modifier = Modifier
        .fillMaxSize()
        .background(Colors.current.surface3),
    splitterViewParams: SplitterViewParams = SplitterViewParams(),
    panelContent: @Composable () -> Unit,
    mainContent: @Composable () -> Unit
) {
    val splitterOrientation = when (direction) {
        PanelDirection.LEFT, PanelDirection.RIGHT -> Orientation.Vertical
        PanelDirection.TOP, PanelDirection.BOTTOM -> Orientation.Horizontal
    }
    val size = remember { mutableStateOf<Dp?>(null) }
    val totalSize = remember { mutableStateOf<Dp?>(null) }

    Layout(
        modifier = modifier,
        content = {
            if (panelEnabled) {
                Box(modifier = panelModifier) { panelContent() }

                DraggableSplitter(
                    orientation = splitterOrientation,
                    params = splitterViewParams,
                    onResize = { delta ->
                        // invert delta for right/bottom dragging
                        val adjustedDelta = when (direction) {
                            PanelDirection.LEFT, PanelDirection.TOP -> delta
                            PanelDirection.RIGHT, PanelDirection.BOTTOM -> -delta
                        }

                        size.value?.let { currentSize ->
                            val max = totalSize.value?.let { params.maxPanelSize(total = it) }
                            val min = totalSize.value?.let { params.minPanelSize(total = it) }
                            size.value = (currentSize + adjustedDelta)
                                .coerceAtLeastNullable(min)
                                .coerceAtMostNullable(max)
                        }
                    }
                )
            }

            Box(modifier = contentModifier) { mainContent() }
        },
        measurePolicy = { measurables, constraints ->
            if (panelEnabled) {
                @Suppress("MagicNumber")
                require(measurables.size == 3)

                val totalSizeDp = when (direction) {
                    PanelDirection.LEFT, PanelDirection.RIGHT -> constraints.maxWidth
                    PanelDirection.BOTTOM, PanelDirection.TOP -> constraints.maxHeight
                }
                    .toDp()
                    .also { totalSize.value = it }

                val panelSizePx = size.value?.roundToPx()
                    ?: params.initialSize.measure(total = totalSizeDp)
                        .coerceAtLeast(params.minPanelSize(total = totalSizeDp))
                        .coerceAtMost(params.maxPanelSize(total = totalSizeDp))
                        .also { size.value = it }
                        .roundToPx()

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
                val contentPlaceable = measurables[2].measure(
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

                val splitterPlaceable = measurables[1].measure(constraints)

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
            } else {
                require(measurables.size == 1)

                val placeable = measurables[0].measure(constraints)
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.place(0, 0)
                }
            }
        }
    )
}
