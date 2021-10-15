package com.dzirbel.kotify.ui.components.table

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.isSpecified
import com.dzirbel.kotify.ui.theme.Colors
import com.dzirbel.kotify.ui.theme.Dimens
import kotlin.math.roundToInt

enum class Sort { IN_ORDER, REVERSE_ORDER }

/**
 * A table layout which renders [items] in a set of [columns]. Each [Column] determines how the content for the row
 * provided by [items] is displayed. Moreover, each [Column] determines the width of its content based on its
 * [Column.width].
 *
 * Columns can optionally display a header row if [includeHeader] is true.
 */
@Composable
@Suppress("UnnecessaryParentheses")
fun <T> Table(
    columns: List<Column<T>>,
    items: List<T>,
    includeHeader: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current

    val numCols = columns.size
    val numRows = items.size + if (includeHeader) 1 else 0
    val numDividers = numRows - 1

    val sortState = remember { mutableStateOf<Pair<Column<T>, Sort>?>(null) }

    // map from original row index to its index when sorted
    val sortedIndexMap: IntArray = remember(sortState.value, items) {
        val indexed = sortState.value?.let { (column, sort) ->
            val comparator = Comparator<IndexedValue<T>> { (firstIndex, first), (secondIndex, second) ->
                column.compare(
                    first = first,
                    firstIndex = firstIndex,
                    second = second,
                    secondIndex = secondIndex
                )
            }.let { if (sort == Sort.REVERSE_ORDER) it.reversed() else it }

            val indexedArray = Array(items.size) { index -> IndexedValue(index = index, value = items[index]) }
            indexedArray.sortWith(comparator)

            IntArray(indexedArray.size) { indexedArray[it].index }
        } ?: IntArray(items.size) { it }

        if (includeHeader) {
            // prepend 0 and increment all the other indexes to account for the header row
            IntArray(indexed.size + 1) { if (it == 0) 0 else indexed[it - 1] + 1 }
        } else {
            indexed
        }
    }

    Layout(
        modifier = modifier,
        content = {
            if (includeHeader) {
                columns.forEach { column ->
                    column.header(
                        sort = remember(column) {
                            object : MutableState<Sort?> {
                                override var value: Sort?
                                    get() = if (sortState.value?.first == column) sortState.value?.second else null
                                    set(value) {
                                        sortState.value = value?.let { Pair(column, it) }
                                    }

                                override fun component1(): Sort? = value

                                override fun component2(): (Sort?) -> Unit = { value = it }
                            }
                        }
                    )
                }
            }

            items.forEachIndexed { index, item ->
                columns.forEach { column ->
                    column.item(item, index)
                }
            }

            repeat(numDividers) {
                Box(Modifier.background(Colors.current.dividerColor))
            }
        },
        measurePolicy = { measurables, constraints ->
            val numNonDividers = measurables.size - numDividers

            // row index -> the indexes of the measurables/placeables for the items in each row
            val indexesForRow: Array<IntRange> = Array(sortedIndexMap.size) { index ->
                val row = sortedIndexMap[index]
                (row * numCols) until ((row + 1) * numCols)
            }

            // column index -> the indexes of the measurables/placeables for the items in each column
            val indexesForCol: Array<IntArray> = Array(numCols) { col ->
                IntArray(numRows) { row -> col + (numCols * row) }
            }

            val columnWidths: Array<ColumnWidth> = Array(numCols) { col -> columns[col].width }
            var remainingWidth: Float = constraints.maxWidth.toFloat()
            // column width in pixels after it has been measured
            val colWidths: Array<Float?> = arrayOfNulls(numCols)
            // index of placeable is the same as the index of the measurable
            val placeables: Array<Placeable?> = arrayOfNulls(numNonDividers)

            // 1: first measure the fixed columns and those determined by header width
            columnWidths.forEachIndexed { colIndex, columnSize ->
                if (columnSize is ColumnWidth.Fixed) {
                    val width = columnSize.width.toPx()
                    colWidths[colIndex] = width
                    remainingWidth -= width

                    indexesForCol[colIndex].forEach { index ->
                        placeables[index] = measurables[index].measure(
                            Constraints.fixedWidth(width = width.roundToInt())
                        )
                    }
                } else if (columnSize is ColumnWidth.MatchHeader) {
                    check(includeHeader) { "cannot use ${ColumnWidth.MatchHeader} without a header" }

                    val headerIndex = indexesForCol[colIndex].first()
                    val headerPlaceable = measurables[headerIndex].measure(Constraints())
                    placeables[headerIndex] = headerPlaceable
                    val width = headerPlaceable.width

                    colWidths[colIndex] = width.toFloat()
                    remainingWidth -= width

                    indexesForCol[colIndex].drop(1).forEach { index ->
                        placeables[index] = measurables[index].measure(
                            Constraints.fixedWidth(width = width)
                        )
                    }
                }
            }

            // 2: then measure the fill columns
            columnWidths.forEachIndexed { colIndex, columnSize ->
                if (columnSize is ColumnWidth.Fill) {
                    val min = if (columnSize.minWidth.isSpecified) columnSize.minWidth.roundToPx() else 0
                    val max = if (columnSize.maxWidth.isSpecified) {
                        columnSize.maxWidth.roundToPx()
                    } else {
                        Constraints.Infinity
                    }

                    val colWidth = indexesForCol[colIndex].maxOf { index ->
                        measurables[index].measure(Constraints(minWidth = min, maxWidth = max))
                            .also { placeables[index] = it }
                            .width
                    }

                    colWidths[colIndex] = colWidth.toFloat()
                    remainingWidth -= colWidth
                }
            }

            // 3: finally measure the weighted columns with the remaining space
            var totalWeight = 0f
            columnWidths.forEach {
                if (it is ColumnWidth.Weighted) {
                    totalWeight += it.weight
                }
            }

            columnWidths.forEachIndexed { colIndex, columnSize ->
                if (columnSize is ColumnWidth.Weighted) {
                    val colWidth = ((columnSize.weight / totalWeight) * remainingWidth)
                    colWidths[colIndex] = colWidth

                    indexesForCol[colIndex].forEach { index ->
                        placeables[index] = measurables[index].measure(
                            Constraints(maxWidth = colWidth.roundToInt().coerceAtLeast(0))
                        )
                    }
                }
            }

            // height of each row is the maximum height of the cell in the group; total height is used for the layout
            var totalHeight = 0
            val rowHeights = Array(numRows) { rowIndex ->
                indexesForRow[rowIndex].maxOf { placeables[it]!!.height }
                    .also { totalHeight += it }
            }

            val dividerHeightPx = Dimens.divider.roundToPx()
            val dividerPlaceables = Array(numDividers) { index ->
                measurables[numNonDividers + index].measure(
                    Constraints.fixed(width = constraints.maxWidth, height = dividerHeightPx)
                ).also { totalHeight += it.height }
            }

            layout(constraints.maxWidth, totalHeight) {
                var y = 0
                var rowIndex = 0

                indexesForRow.forEach { rowIndexes ->
                    var col = 0
                    var x = 0
                    rowIndexes.forEach { placeableIndex ->
                        val placeable = placeables[placeableIndex]!!

                        val alignment = if (rowIndex == 0 && includeHeader) {
                            columns[col].headerAlignment
                        } else {
                            columns[col].cellAlignment
                        }

                        val colWidth = colWidths[col]!!.roundToInt()
                        val offset = alignment.align(
                            size = IntSize(width = placeable.width, height = placeable.height),
                            space = IntSize(width = colWidth, height = rowHeights[rowIndex]),
                            layoutDirection = layoutDirection,
                        )

                        placeable.place(x = x + offset.x, y = y + offset.y)

                        x += colWidth
                        col++
                    }

                    y += rowHeights[rowIndex]

                    if (rowIndex < numDividers) {
                        val dividerPlaceable = dividerPlaceables[rowIndex]
                        dividerPlaceable.place(x = 0, y = y)
                        y += dividerPlaceable.height
                    }

                    rowIndex++
                }
            }
        }
    )
}
