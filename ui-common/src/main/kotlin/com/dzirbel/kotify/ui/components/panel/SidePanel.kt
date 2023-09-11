package com.dzirbel.kotify.ui.components.panel

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
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.util.coerceAtLeastNullable
import com.dzirbel.kotify.util.coerceAtMostNullable

/**
 * A layout which contains two elements, [mainContent] and [panelContent] with the panel placed relative to the main
 * content based on [direction] and sized based on [panelSize]. A [DraggableSplitter] is placed between them configured
 * by [splitterViewParams].
 */
@Composable
fun SidePanel(
    direction: PanelDirection,
    panelSize: PanelSize,
    panelContent: @Composable () -> Unit,
    modifier: Modifier = Modifier.fillMaxSize(),
    panelModifier: Modifier = Modifier.fillMaxSize(),
    contentModifier: Modifier = Modifier.fillMaxSize(),
    panelEnabled: Boolean = true,
    dragTargetSize: Dp = Dimens.space3,
    mainContent: @Composable () -> Unit,
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
                    dragTargetSize = dragTargetSize,
                    onResize = { delta ->
                        // invert delta for right/bottom dragging
                        val adjustedDelta = when (direction) {
                            PanelDirection.LEFT, PanelDirection.TOP -> delta
                            PanelDirection.RIGHT, PanelDirection.BOTTOM -> -delta
                        }

                        size.value?.let { currentSize ->
                            val max = totalSize.value?.let { panelSize.maxPanelSize(total = it) }
                            val min = totalSize.value?.let { panelSize.minPanelSize(total = it) }
                            size.value = (currentSize + adjustedDelta)
                                .coerceAtLeastNullable(min)
                                .coerceAtMostNullable(max)
                        }
                    },
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
                    ?: panelSize.initialSize.measure(total = totalSizeDp)
                        .coerceAtLeast(panelSize.minPanelSize(total = totalSizeDp))
                        .coerceAtMost(panelSize.maxPanelSize(total = totalSizeDp))
                        .also { size.value = it }
                        .roundToPx()

                val panelPlaceable = measurables[0].measure(
                    when (splitterOrientation) {
                        Orientation.Horizontal -> constraints.copy(minHeight = panelSizePx, maxHeight = panelSizePx)
                        Orientation.Vertical -> constraints.copy(minWidth = panelSizePx, maxWidth = panelSizePx)
                    },
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
                                maxHeight = contentSize,
                            )
                        Orientation.Vertical ->
                            Constraints(
                                minWidth = contentSize,
                                maxWidth = contentSize,
                                minHeight = constraints.maxHeight,
                                maxHeight = constraints.maxHeight,
                            )
                    },
                )

                val splitterPlaceable = measurables[1].measure(constraints.copy(minWidth = 0, minHeight = 0))

                layout(constraints.maxWidth, constraints.maxHeight) {
                    // ensure the content is always placed before the panel to allow shadows from the panel
                    when (direction) {
                        PanelDirection.LEFT -> {
                            contentPlaceable.place(panelPlaceable.width, 0)
                            panelPlaceable.place(0, 0)
                            splitterPlaceable.place(panelPlaceable.width - splitterPlaceable.width / 2, 0)
                        }
                        PanelDirection.RIGHT -> {
                            contentPlaceable.place(0, 0)
                            panelPlaceable.place(contentPlaceable.width, 0)
                            // do not center splitter on the panel boundary since it might be on top of a scrollbar
                            splitterPlaceable.place(contentPlaceable.width, 0)
                        }
                        PanelDirection.TOP -> {
                            contentPlaceable.place(0, panelPlaceable.height)
                            panelPlaceable.place(0, 0)
                            splitterPlaceable.place(0, panelPlaceable.height - splitterPlaceable.height / 2)
                        }
                        PanelDirection.BOTTOM -> {
                            contentPlaceable.place(0, 0)
                            panelPlaceable.place(0, contentPlaceable.height)
                            splitterPlaceable.place(0, contentPlaceable.height - splitterPlaceable.height / 2)
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
        },
    )
}
