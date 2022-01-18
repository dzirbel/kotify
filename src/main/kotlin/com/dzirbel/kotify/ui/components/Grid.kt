package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import com.dzirbel.kotify.ui.theme.Dimens
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A simple two-dimensional grid layout, which arranges [elementContent] for each [elements] as a table.
 *
 * The layout always expands vertically to fit all the [elements]. Each column has the width of the widest
 * [elementContent]; each row the height of the tallest [elementContent] in that row. The number of columns will equal
 * [columns] if provided, otherwise it will be the maximum number of contents that can fit.
 *
 * [horizontalSpacing] and [verticalSpacing] will be added between columns and rows, respectively, including before the
 * first row/column and after the last row/column.
 */
@Composable
@Suppress("UnnecessaryParentheses")
fun <E> Grid(
    elements: List<E>,
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = Dimens.space3,
    verticalSpacing: Dp = Dimens.space3,
    cellAlignment: Alignment = Alignment.Center,
    columns: Int? = null,
    elementContent: @Composable (E) -> Unit,
) {
    require(columns == null || columns > 0) { "columns must be positive; got $columns" }
    val layoutDirection = LocalLayoutDirection.current

    Layout(
        content = {
            elements.forEach { element ->
                Box {
                    elementContent(element)
                }
            }
        },
        modifier = modifier,
        measurePolicy = { measurables, constraints ->
            check(measurables.size == elements.size)

            val horizontalSpacingPx: Float = horizontalSpacing.toPx()
            val verticalSpacingPx: Float = verticalSpacing.toPx()

            // max width for each column is the total column space (total width minus one horizontal spacing for the
            // spacing after the last column) divided by the minimum number of columns, minus the spacing for the column
            val minColumns = columns ?: 1
            val elementConstraints = Constraints(
                maxWidth = (((constraints.maxWidth - horizontalSpacingPx) / minColumns) - horizontalSpacingPx).toInt()
                    .coerceAtLeast(0)
            )

            var maxElementWidth = 0 // find max element width while measuring to avoid an extra loop
            val placeables = measurables.map {
                it.measure(elementConstraints).also { placeable ->
                    maxElementWidth = max(maxElementWidth, placeable.width)
                }
            }

            // the total width of a column, including its spacing
            val columnWidthWithSpacing: Float = maxElementWidth + horizontalSpacingPx

            // number of columns is the total column space (total width minus one horizontal spacing for the spacing
            // after the last column) divided by the column width including its spacing; then taking the floor to
            // truncate any "fractional column"
            val cols: Int = columns
                ?: ((constraints.maxWidth - horizontalSpacingPx) / columnWidthWithSpacing).toInt().coerceAtLeast(1)
            // number of rows is the number of elements divided by number of columns, rounded up
            val rows: Int = ceil(elements.size.toFloat() / cols).toInt()

            // now we need to account for that "fractional column" by adding some "extra" to each column spacing,
            // distributed among each spacing (note: we cannot add this extra to the columns rather than the spacing
            // because the placeables have already been measured)
            // first: the total width used without the extra is the number of columns times the column width with
            // spacing plus an extra horizontal spacing to account for the trailing space
            // next: extra is the max width minus the used width, divided by the number of columns plus one (to include
            // the trailing space)
            // finally: create adjusted width variables including the extra
            val usedWidth: Float = (cols * columnWidthWithSpacing) + horizontalSpacingPx
            val extra: Float = (constraints.maxWidth - usedWidth) / (cols + 1)
            val horizontalSpacingPxWithExtra: Float = horizontalSpacingPx + extra
            val columnWidthWithSpacingAndExtra: Float = maxElementWidth + horizontalSpacingPxWithExtra

            // total used height is the sum of the row heights (each of which being the maximum element height in the
            // row) plus the total vertical spacing (the vertical spacing per row times the number of rows plus 1, to
            // include the trailing space)
            var totalHeight = (verticalSpacingPx * (rows + 1)).roundToInt()
            val rowHeights = Array(rows) { row ->
                placeables.subList(fromIndex = row * cols, toIndex = ((row + 1) * cols).coerceAtMost(elements.size))
                    .maxOf { it.height }
                    .also { totalHeight += it }
            }

            layout(constraints.maxWidth, totalHeight) {
                // keep track of the y for each row; start at the spacing to include the top spacing
                var y = verticalSpacingPx
                for (rowIndex in 0 until rows) {
                    val rowHeight = rowHeights[rowIndex]
                    val roundedY = y.roundToInt()

                    for (colIndex in 0 until cols) {
                        placeables.getOrNull(colIndex + rowIndex * cols)?.let { placeable ->
                            val baseX = (horizontalSpacingPxWithExtra + (colIndex * columnWidthWithSpacingAndExtra))
                                .roundToInt()

                            // then adjust the element based on its alignment and place it
                            val alignment = cellAlignment.align(
                                size = IntSize(width = placeable.width, height = placeable.height),
                                space = IntSize(width = maxElementWidth, height = rowHeight),
                                layoutDirection = layoutDirection,
                            )

                            placeable.place(x = baseX + alignment.x, y = roundedY + alignment.y)
                        }
                    }

                    y += rowHeight + verticalSpacingPx
                }
            }
        }
    )
}
