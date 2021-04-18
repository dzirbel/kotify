package com.dzirbel.kotify.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.dzirbel.kotify.ui.theme.Dimens
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A simple two dimensional grid layout, which arranges [elementContent] for each [elements] as a table.
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
    horizontalCellAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalCellAlignment: Alignment.Vertical = Alignment.CenterVertically,
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    columns: Int? = null,
    elementContent: @Composable (E) -> Unit
) {
    require(elements.isNotEmpty()) // TODO allow empty
    require(columns == null || columns > 0)

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

            val horizontalSpacingPx = horizontalSpacing.toPx()
            val verticalSpacingPx = verticalSpacing.roundToPx()

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
            val columnWidthWithSpacing = maxElementWidth + horizontalSpacingPx

            // number of columns is the total column space (total width minus one horizontal spacing for the spacing
            // after the last column) divided by the column width including its spacing; then taking the floor to
            // truncate any "fractional column"
            val cols = columns
                ?: floor((constraints.maxWidth - horizontalSpacingPx) / columnWidthWithSpacing).toInt().coerceAtLeast(1)

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

            // chunk placeables into rows
            val chunkedPlaceables = placeables.chunked(cols)

            // total used height is the sum of the row heights (each of which being the maximum element height in the
            // row) plus the total vertical spacing (the vertical spacing per row times the number of rows plus 1, to
            // include the trailing space)
            val totalHeight = (verticalSpacingPx * (chunkedPlaceables.size + 1)) +
                chunkedPlaceables.sumBy { row -> row.maxOf { it.height } }

            layout(constraints.maxWidth, totalHeight) {
                // keep track of the y for each row; start at the spacing to include the top spacing
                var y = verticalSpacingPx
                for (row in chunkedPlaceables) {
                    val rowHeight = row.maxOf { it.height }

                    for ((colIndex, placeable) in row.withIndex()) {
                        // now, to position the element: (baseX, baseY) is the the top-left corner of its cell
                        val baseX = (horizontalSpacingPxWithExtra + (colIndex * columnWidthWithSpacingAndExtra))
                            .roundToInt()
                        val baseY = y

                        // then adjust the element based on its alignment and place it
                        placeable.place(
                            x = baseX + horizontalCellAlignment.align(
                                size = placeable.width,
                                space = maxElementWidth,
                                layoutDirection = layoutDirection
                            ),
                            y = baseY + verticalCellAlignment.align(
                                size = placeable.height,
                                space = rowHeight
                            )
                        )
                    }

                    y += rowHeight + verticalSpacingPx
                }
            }
        }
    )
}
