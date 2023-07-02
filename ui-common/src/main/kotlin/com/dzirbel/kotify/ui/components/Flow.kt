package com.dzirbel.kotify.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.instrumentation.instrument
import kotlin.math.max
import kotlin.math.min

/**
 * A horizontal row layout which flows overflowing [content] into new rows, like text.
 *
 * [horizontalSpacing] is placed horizontally between elements; [verticalSpacing] between rows. Content is aligned
 * within each row by [verticalAlignment].
 */
@Composable
fun Flow(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = Dimens.space2,
    verticalSpacing: Dp = Dimens.space2,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable () -> Unit,
) {
    Layout(
        content = content,
        modifier = modifier.instrument(),
        measurePolicy = { measurables, constraints ->
            val horizontalSpacingPx = horizontalSpacing.roundToPx()
            val verticalSpacingPx = verticalSpacing.roundToPx()

            var currentRowWidth = 0
            var currentRowHeight = 0
            var maxRowWidth = 0
            var totalHeight = 0
            val rowHeights = mutableListOf<Int>()
            val placeables = measurables.map { measurable ->
                val placeable = measurable.measure(constraints = constraints)

                if (currentRowWidth + placeable.width > constraints.maxWidth) {
                    rowHeights.add(currentRowHeight)
                    totalHeight += currentRowHeight + verticalSpacingPx
                    maxRowWidth = max(maxRowWidth, currentRowWidth - horizontalSpacingPx)

                    currentRowWidth = 0
                    currentRowHeight = 0
                }

                currentRowWidth += placeable.width + horizontalSpacingPx
                currentRowHeight = max(currentRowHeight, placeable.height)

                placeable
            }

            rowHeights.add(currentRowHeight)
            totalHeight += currentRowHeight
            maxRowWidth = max(maxRowWidth, currentRowWidth - horizontalSpacingPx)

            layout(width = maxRowWidth, height = min(totalHeight, constraints.maxHeight)) {
                var x = 0
                var y = 0
                var rowIndex = 0
                var rowHeight = rowHeights[rowIndex]
                placeables.forEach { placeable ->
                    if (x + placeable.width > constraints.maxWidth) {
                        x = 0
                        y += rowHeight + verticalSpacingPx

                        rowIndex++
                        rowHeight = rowHeights[rowIndex]
                    }

                    placeable.place(
                        x = x,
                        y = y + verticalAlignment.align(size = placeable.height, space = rowHeight),
                    )

                    x += placeable.width + horizontalSpacingPx
                }
            }
        },
    )
}
