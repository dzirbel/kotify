package com.dzirbel.kotify.ui.components.table

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.isSpecified
import com.dzirbel.kotify.ui.components.adapter.ListAdapter
import com.dzirbel.kotify.ui.components.adapter.Sort
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.util.coerceAtLeastNullable
import com.dzirbel.kotify.util.sumOfNullable
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.roundToInt

/**
 * A table layout which renders [items] in a set of [columns]. Each [Column] determines how the content for the row
 * provided by [items] is displayed. Moreover, each [Column] determines the width of its content based on its
 * [Column.width].
 *
 * Columns can optionally display a header row if [includeHeader] is true.
 *
 * TODO support adapter divisions in a table (currently the divider content is not inserted between them)
 */
@Composable
@Suppress("UnnecessaryParentheses", "UnsafeCallOnNullableType")
fun <E> Table(
    columns: ImmutableList<Column<E>>,
    items: ListAdapter<E>,
    modifier: Modifier = Modifier,
    includeHeader: Boolean = true,
    onSetSort: (Sort<E>?) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current

    val numCols = columns.size
    val numRows = items.size
    val numDividers = (numRows + if (includeHeader) 1 else 0) - 1

    val divisions = items.divisions

    Layout(
        modifier = modifier,
        content = {
            if (includeHeader) {
                columns.forEach { column: Column<E> ->
                    Box {
                        column.Header(
                            sortOrder = items.sortOrderFor(column.sortableProperty),
                            onSetSort = { sortOrder ->
                                onSetSort(sortOrder?.let { _ -> column.sortableProperty?.let { Sort(it, sortOrder) } })
                            },
                        )
                    }
                }
            }

            items.forEach { item ->
                columns.forEach { column ->
                    Box {
                        column.Item(item)
                    }
                }
            }

            repeat(numDividers) {
                Box(Modifier.background(LocalColors.current.dividerColor))
            }
        },
        measurePolicy = { measurables, constraints ->
            val headerMeasurables = if (includeHeader) measurables.subList(fromIndex = 0, toIndex = numCols) else null
            var measurablesIndex = headerMeasurables?.size ?: 0
            val cellMeasurables = measurables.subList(
                fromIndex = measurablesIndex,
                toIndex = measurablesIndex + numRows * numCols,
            )
            measurablesIndex += cellMeasurables.size
            val dividerMeasurables = measurables.subList(
                fromIndex = measurablesIndex,
                toIndex = measurablesIndex + numDividers,
            )
            measurablesIndex += numDividers
            check(measurablesIndex == measurables.size) // all measurables have been accounted for

            // computes the index in cellMeasurables/cellPlaceables for the given row/col indices
            fun cellPlaceablesIndex(row: Int, col: Int): Int = col + (numCols * row)

            // total width used by non-weighted columns, to distribute the weighted ones
            var usedWidth = 0f
            // column width in pixels after it has been measured
            val colWidths: Array<Float?> = arrayOfNulls(numCols)

            val headerPlaceables: Array<Placeable?>? = headerMeasurables?.let { arrayOfNulls(it.size) }
            val cellPlaceables = arrayOfNulls<Placeable>(cellMeasurables.size)

            // 1: first measure the fixed columns and those determined by header width
            columns.forEachIndexed { colIndex, column ->
                val columnWidth = column.width
                if (columnWidth is ColumnWidth.Fixed) {
                    val width = columnWidth.width.toPx()
                    colWidths[colIndex] = width
                    usedWidth += width

                    val colConstraints = Constraints(maxWidth = width.roundToInt())
                    if (includeHeader) {
                        headerPlaceables!![colIndex] = headerMeasurables[colIndex].measure(colConstraints)
                    }

                    repeat(numRows) { row ->
                        val index = cellPlaceablesIndex(row = row, col = colIndex)
                        cellPlaceables[index] = cellMeasurables[index].measure(colConstraints)
                    }
                } else if (columnWidth is ColumnWidth.MatchHeader) {
                    check(includeHeader) { "cannot use ${ColumnWidth.MatchHeader} without a header" }

                    val headerPlaceable = headerMeasurables!![colIndex].measure(Constraints())
                    headerPlaceables!![colIndex] = headerPlaceable
                    val headerWidth = headerPlaceable.width

                    colWidths[colIndex] = headerWidth.toFloat()
                    usedWidth += headerWidth

                    val colConstraints = Constraints(maxWidth = headerWidth)
                    repeat(numRows) { row ->
                        val index = cellPlaceablesIndex(row = row, col = colIndex)
                        cellPlaceables[index] = cellMeasurables[index].measure(colConstraints)
                    }
                }
            }

            // 2: then measure the fill columns
            columns.forEachIndexed { colIndex, column ->
                val columnWidth = column.width
                if (columnWidth is ColumnWidth.Fill) {
                    val min = if (columnWidth.minWidth.isSpecified) columnWidth.minWidth.roundToPx() else 0
                    val max = if (columnWidth.maxWidth.isSpecified) {
                        columnWidth.maxWidth.roundToPx()
                    } else {
                        Constraints.Infinity
                    }
                    val colConstraints = Constraints(minWidth = min, maxWidth = max)

                    val maxCellWidth = (0 until numRows).maxOfOrNull { row ->
                        val index = cellPlaceablesIndex(row = row, col = colIndex)
                        cellMeasurables[index].measure(colConstraints)
                            .also { cellPlaceables[index] = it }
                            .width
                    }
                        ?: columnWidth.minWidth.takeIf { it.isSpecified }?.roundToPx()
                        ?: 0

                    val colWidth = maxCellWidth
                        .coerceAtLeastNullable(
                            headerMeasurables?.get(colIndex)?.measure(colConstraints)
                                ?.also { headerPlaceables!![colIndex] = it }
                                ?.width,
                        )

                    colWidths[colIndex] = colWidth.toFloat()
                    usedWidth += colWidth
                }
            }

            // 3: finally measure the weighted columns with the remaining space
            val totalWeight = columns.sumOfNullable { (it.width as? ColumnWidth.Weighted)?.weight }
            if (totalWeight > 0) {
                val remainingWidth = constraints.maxWidth - usedWidth
                columns.forEachIndexed { colIndex, column ->
                    val columnWidth = column.width
                    if (columnWidth is ColumnWidth.Weighted) {
                        val colWidth = (columnWidth.weight / totalWeight) * remainingWidth
                        colWidths[colIndex] = colWidth
                        val colConstraints = Constraints(maxWidth = colWidth.roundToInt().coerceAtLeast(0))

                        if (includeHeader) {
                            headerPlaceables!![colIndex] = headerMeasurables[colIndex].measure(colConstraints)
                        }

                        repeat(numRows) { row ->
                            val index = cellPlaceablesIndex(row = row, col = colIndex)
                            cellPlaceables[index] = cellMeasurables[index].measure(colConstraints)
                        }
                    }
                }
                usedWidth = constraints.maxWidth.toFloat()
            }

            // array values should be initialized by now; avoid null assertions
            @Suppress("UNCHECKED_CAST")
            cellPlaceables as Array<Placeable>
            @Suppress("UNCHECKED_CAST", "CastToNullableType")
            headerPlaceables as Array<Placeable>?
            @Suppress("UNCHECKED_CAST")
            colWidths as Array<Float>

            // height of each row is the maximum height of the cell in the group; total height is used for the layout
            var totalHeight = 0

            val headerHeight = headerPlaceables?.maxOf { it.height } ?: 0
            totalHeight += headerHeight

            val divisionElements = divisions.values.toList()

            // division -> [heights of rows in that division]
            val rowHeights: Array<IntArray> = Array(divisions.size) { divisionIndex ->
                val division = divisionElements[divisionIndex]

                IntArray(division.size) { row ->
                    (0 until numCols).maxOf { col ->
                        cellPlaceables[cellPlaceablesIndex(row = division[row].index, col = col)].height
                    }
                        .also { totalHeight += it }
                }
            }

            val dividerHeightPx = Dimens.divider.roundToPx()
            val dividerConstraints = Constraints.fixed(width = usedWidth.roundToInt(), height = dividerHeightPx)
            val dividerPlaceables = Array(numDividers) { index ->
                dividerMeasurables[index].measure(dividerConstraints)
                    .also { totalHeight += it.height }
            }

            layout(usedWidth.roundToInt(), totalHeight) {
                var y = 0
                var dividerIndex = 0

                if (includeHeader) {
                    var headerX = 0
                    headerPlaceables?.forEachIndexed { col, placeable ->
                        val colWidth = colWidths[col].roundToInt()
                        val offset = columns[col].headerAlignment.align(
                            size = IntSize(width = placeable.width, height = placeable.height),
                            space = IntSize(width = colWidth, height = headerHeight),
                            layoutDirection = layoutDirection,
                        )

                        placeable.place(x = headerX + offset.x, y = offset.y)

                        headerX += colWidth
                    }

                    y += headerHeight

                    // place divider under the header
                    if (dividerPlaceables.isNotEmpty()) {
                        val dividerPlaceable = dividerPlaceables[dividerIndex]
                        dividerPlaceable.place(x = 0, y = y)
                        y += dividerPlaceable.height
                        dividerIndex++
                    }
                }

                divisionElements.forEachIndexed { divisionIndex, division ->
                    rowHeights[divisionIndex].forEachIndexed { rowIndex, rowHeight ->
                        val elementIndex = division[rowIndex].index
                        var x = 0
                        repeat(numCols) { colIndex ->
                            val placeable = cellPlaceables[cellPlaceablesIndex(row = elementIndex, col = colIndex)]

                            val colWidth = colWidths[colIndex].roundToInt()
                            val offset = columns[colIndex].cellAlignment.align(
                                size = IntSize(width = placeable.width, height = placeable.height),
                                space = IntSize(width = colWidth, height = rowHeight),
                                layoutDirection = layoutDirection,
                            )

                            placeable.place(x = x + offset.x, y = y + offset.y)

                            x += colWidth
                        }

                        y += rowHeight

                        if (dividerIndex < numDividers) {
                            val dividerPlaceable = dividerPlaceables[dividerIndex]
                            dividerPlaceable.place(x = 0, y = y)
                            y += dividerPlaceable.height
                            dividerIndex++
                        }
                    }
                }
            }
        },
    )
}
