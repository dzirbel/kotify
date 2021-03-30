package com.dominiczirbel.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.dominiczirbel.network.model.PlaylistTrack
import com.dominiczirbel.ui.theme.Colors
import com.dominiczirbel.ui.theme.Dimens
import kotlin.math.roundToInt

/**
 * A simple [Column] where the header and content are rendered as [Text] via [toString].
 */
abstract class ColumnByString<T>(
    val header: String,
    override val width: ColumnWidth,
    val padding: PaddingValues = PaddingValues(Dimens.space2),
    override val horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    override val verticalAlignment: Alignment.Vertical = Alignment.Top
) : Column<T> {
    /**
     * Renders the content of the given [item] as the returned string.
     */
    abstract fun toString(item: T, index: Int): String

    override fun compare(first: T, firstIndex: Int, second: T, secondIndex: Int): Int {
        return toString(first, firstIndex).compareTo(toString(second, secondIndex))
    }

    @Composable
    override fun header() {
        Text(text = header, fontWeight = FontWeight.Bold, modifier = Modifier.padding(padding))
    }

    @Composable
    override fun item(item: T, index: Int) {
        Text(text = toString(item, index), modifier = Modifier.padding(padding))
    }
}

object IndexColumn : ColumnByString<PlaylistTrack>(header = "#", width = ColumnWidth.Fill()) {
    override fun toString(item: PlaylistTrack, index: Int) = (index + 1).toString()
}

/**
 * Determines how to measure the width of a column in a [Table].
 */
sealed class ColumnWidth {
    /**
     * A fixed-width column with the given [width].
     */
    class Fixed(val width: Dp) : ColumnWidth()

    /**
     * A column whose contents fill the maximum space, up to [maxWidth] and with an optional [minWidth]. The width of
     * the column will be the maximum of the width of the cells in the column (including the header).
     */
    class Fill(val minWidth: Dp = Dp.Unspecified, val maxWidth: Dp = Dp.Unspecified) : ColumnWidth()

    /**
     * A column whose width is weighted among the space remaining after [Fixed] and [Fill] columns are allocated. The
     * remaining width is split between the weighted columns by their [weight].
     */
    class Weighted(val weight: Float) : ColumnWidth()
}

/**
 * Represents a single column in a [Table].
 */
interface Column<T> {
    /**
     * The policy by which the width of the column is measured.
     */
    val width: ColumnWidth

    /**
     * The way that items in this [Column] are aligned horizontally within their cell.
     */
    val horizontalAlignment: Alignment.Horizontal
        get() = Alignment.Start

    /**
     * The way that the header of this [Column] is aligned horizontally within its cell.
     */
    val headerHorizontalAlignment: Alignment.Horizontal
        get() = Alignment.Start

    /**
     * The way that items in this [Column] are aligned vertically within their cell.
     */
    val verticalAlignment: Alignment.Vertical
        get() = Alignment.Top

    /**
     * The way that the header of this [Column] is aligned vertically within its cell.
     */
    val headerVerticalAlignment: Alignment.Vertical
        get() = Alignment.Bottom

    /**
     * Compares two elements in the column, returning a negative number if [first] should be placed before [second] and
     * a positive number if [second] should be placed before [first]. Should never return zero.
     */
    fun compare(first: T, firstIndex: Int, second: T, secondIndex: Int): Int

    /**
     * The content for the header of this column.
     */
    @Composable
    fun header()

    /**
     * The content for the given [item] at the given [index].
     */
    @Composable
    fun item(item: T, index: Int)

    /**
     * Creates a new [Column] from this [Column] with the same values, but mapped with [mapper]. This is convenient for
     * reusing a [Column] with a different type of item but the same content.
     */
    fun <R> mapped(mapper: (R) -> T): Column<R> {
        val base = this
        return object : Column<R> {
            override val width = base.width
            override val horizontalAlignment = base.horizontalAlignment
            override val headerHorizontalAlignment = base.headerHorizontalAlignment
            override val verticalAlignment = base.verticalAlignment
            override val headerVerticalAlignment = base.headerVerticalAlignment

            override fun compare(first: R, firstIndex: Int, second: R, secondIndex: Int): Int {
                return base.compare(
                    first = mapper(first),
                    firstIndex = firstIndex,
                    second = mapper(second),
                    secondIndex = secondIndex
                )
            }

            @Composable
            override fun header() {
                base.header()
            }

            @Composable
            override fun item(item: R, index: Int) {
                base.item(mapper(item), index)
            }
        }
    }
}

/**
 * A table layout which lays out [items] in a set of [columns].
 *
 * Unliked [Grid], the number of columns is set and the size of each column is determined by its [Column.width] while
 * its content is determined by the [Column.item].
 */
@Composable
@Suppress("UnnecessaryParentheses")
fun <T> Table(
    columns: List<Column<T>>,
    items: List<T>,
    includeHeader: Boolean = true,
    modifier: Modifier = Modifier,
    layoutDirection: LayoutDirection = LocalLayoutDirection.current
) {
    val numCols = columns.size
    val numRows = items.size + if (includeHeader) 1 else 0
    val numDividers = numRows - 1

    Layout(
        modifier = modifier,
        content = {
            if (includeHeader) {
                columns.forEach { column ->
                    column.header()
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
            val gridMeasurables = measurables.dropLast(numDividers)
            val dividerMeasurables = measurables.takeLast(numDividers)

            // index -> the indexes of the measurables/placeables for the items in each row
            val indexesForRow: List<IntRange> = (0 until numRows).map { row ->
                (row * numCols) until ((row + 1) * numCols)
            }

            check(indexesForRow.size == numRows)
            indexesForRow.forEach { check(it.count() == numCols) }

            // index -> the indexes of the measurables/placeables for the items in each column
            val indexesForCol: List<List<Int>> = (0 until numCols).map { col ->
                List(numRows) { row -> col + (numCols * row) }
            }

            check(indexesForCol.size == numCols)
            indexesForCol.forEach { check(it.count() == numRows) }

            val columnWidths: List<ColumnWidth> = columns.map { it.width }
            var remainingWidth: Float = constraints.maxWidth.toFloat()
            // column width in pixels after it has been measured
            val colWidths: Array<Float?> = arrayOfNulls(numCols)
            // index of placeable is the same as the index of the measurable
            val placeables: Array<Placeable?> = arrayOfNulls(gridMeasurables.size)

            // 1: first measure the fixed columns
            columnWidths.forEachIndexed { colIndex, columnSize ->
                if (columnSize is ColumnWidth.Fixed) {
                    val width = columnSize.width.toPx()
                    colWidths[colIndex] = width
                    remainingWidth -= width

                    indexesForCol[colIndex].forEach { index ->
                        placeables[index] = gridMeasurables[index].measure(
                            Constraints.fixedWidth(width = width.roundToInt())
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
                        gridMeasurables[index].measure(Constraints(minWidth = min, maxWidth = max))
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
                        placeables[index] = gridMeasurables[index].measure(
                            Constraints(maxWidth = colWidth.roundToInt())
                        )
                    }
                }
            }

            // all column widths and placeables should be set by now
            colWidths.forEach { checkNotNull(it) }
            placeables.forEach { checkNotNull(it) }

            // height of each row is the maximum height of the cell in the group; total height is used for the layout
            var totalHeight = 0
            val rowHeights = (0 until numRows).map { rowIndex ->
                indexesForRow[rowIndex].maxOf { placeables[it]!!.height }
                    .also { totalHeight += it }
            }

            val dividerPlaceables = dividerMeasurables.map { dividerMeasurable ->
                dividerMeasurable.measure(
                    Constraints.fixed(width = constraints.maxWidth, height = 1.dp.roundToPx())
                ).also { totalHeight += it.height }
            }

            layout(constraints.maxWidth, totalHeight) {
                var y = 0
                var rowIndex = 0
                placeables.toList().chunked(size = numCols) { row ->
                    var col = 0
                    var x = 0
                    row.forEach { placeable ->
                        val horizontalAlignment = if (rowIndex == 0 && includeHeader) {
                            columns[col].headerHorizontalAlignment
                        } else {
                            columns[col].horizontalAlignment
                        }

                        val verticalAlignment = if (rowIndex == 0 && includeHeader) {
                            columns[col].headerVerticalAlignment
                        } else {
                            columns[col].verticalAlignment
                        }

                        val colWidth = colWidths[col]!!.roundToInt()
                        placeable!!.place(
                            x = x + horizontalAlignment.align(
                                size = placeable.width,
                                space = colWidth,
                                layoutDirection = layoutDirection
                            ),
                            y = y + verticalAlignment.align(
                                size = placeable.height,
                                space = rowHeights[rowIndex]
                            )
                        )

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
