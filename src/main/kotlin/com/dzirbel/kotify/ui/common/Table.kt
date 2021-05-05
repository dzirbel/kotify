package com.dzirbel.kotify.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.isSpecified
import com.dzirbel.kotify.network.model.PlaylistTrack
import com.dzirbel.kotify.ui.theme.Colors
import com.dzirbel.kotify.ui.theme.Dimens
import kotlin.math.roundToInt

/**
 * A simple [Column] where the header and content are rendered as [Text] via [toString].
 */
abstract class ColumnByString<T>(
    val header: String,
    override val width: ColumnWidth,
    val padding: Dp = Dimens.space3,
    val sortable: Boolean = true,
    override val horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    override val verticalAlignment: Alignment.Vertical = Alignment.Top
) : Column<T>() {
    /**
     * Renders the content of the given [item] as the returned string.
     */
    abstract fun toString(item: T, index: Int): String

    override fun compare(first: T, firstIndex: Int, second: T, secondIndex: Int): Int {
        return toString(first, firstIndex).compareTo(toString(second, secondIndex), ignoreCase = true)
    }

    @Composable
    override fun header(sort: MutableState<Sort?>) {
        standardHeader(sort = sort, header = header, padding = padding, sortable = sortable)
    }

    @Composable
    override fun item(item: T, index: Int) {
        Text(text = toString(item, index), modifier = Modifier.padding(padding))
    }
}

/**
 * A simple [Column] where the content is rendered as [Text] via [toNumber]. Used rather than [ColumnByString] to also
 * correctly sort by [toNumber].
 */
abstract class ColumnByNumber<T>(
    val header: String,
    override val width: ColumnWidth,
    val padding: Dp = Dimens.space3,
    val sortable: Boolean = true,
    override val horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    override val verticalAlignment: Alignment.Vertical = Alignment.Top
) : Column<T>() {
    abstract fun toNumber(item: T, index: Int): Number?

    override fun compare(first: T, firstIndex: Int, second: T, secondIndex: Int): Int {
        val firstNumber = toNumber(first, firstIndex)?.toDouble() ?: 0.0
        val secondNumber = toNumber(second, secondIndex)?.toDouble() ?: 0.0
        return firstNumber.compareTo(secondNumber)
    }

    @Composable
    override fun header(sort: MutableState<Sort?>) {
        standardHeader(sort = sort, header = header, padding = padding, sortable = sortable)
    }

    @Composable
    override fun item(item: T, index: Int) {
        Text(text = toNumber(item, index)?.toString().orEmpty(), modifier = Modifier.padding(padding))
    }
}

abstract class ColumnByRelativeDateText<T>(
    val header: String,
    override val width: ColumnWidth,
    val padding: Dp = Dimens.space3,
    val sortable: Boolean = true,
    override val horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    override val verticalAlignment: Alignment.Vertical = Alignment.Top
) : Column<T>() {
    abstract fun relativeDate(item: T, index: Int): Long?

    override fun compare(first: T, firstIndex: Int, second: T, secondIndex: Int): Int {
        return (relativeDate(first, firstIndex) ?: 0).compareTo(relativeDate(second, secondIndex) ?: 0)
    }

    @Composable
    override fun header(sort: MutableState<Sort?>) {
        standardHeader(sort = sort, header = header, padding = padding, sortable = sortable)
    }

    @Composable
    override fun item(item: T, index: Int) {
        val text = relativeDate(item, index)?.let { liveRelativeDateText(timestamp = it) }.orEmpty()
        Text(text = text, modifier = Modifier.padding(Dimens.space3))
    }
}

object IndexColumn : ColumnByNumber<PlaylistTrack>(header = "#", width = ColumnWidth.Fill(), sortable = false) {
    override fun toNumber(item: PlaylistTrack, index: Int) = index + 1
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

    /**
     * A column whose header determines the width of the column.
     */
    object MatchHeader : ColumnWidth()
}

enum class Sort { IN_ORDER, REVERSE_ORDER }

/**
 * Represents a single column in a [Table].
 */
abstract class Column<T> {
    /**
     * The policy by which the width of the column is measured.
     */
    abstract val width: ColumnWidth

    /**
     * The way that items in this [Column] are aligned horizontally within their cell.
     */
    open val horizontalAlignment: Alignment.Horizontal = Alignment.Start

    /**
     * The way that the header of this [Column] is aligned horizontally within its cell.
     */
    open val headerHorizontalAlignment: Alignment.Horizontal = Alignment.Start

    /**
     * The way that items in this [Column] are aligned vertically within their cell.
     */
    open val verticalAlignment: Alignment.Vertical = Alignment.Top

    /**
     * The way that the header of this [Column] is aligned vertically within its cell.
     */
    open val headerVerticalAlignment: Alignment.Vertical = Alignment.Bottom

    /**
     * Compares two elements in the column, returning a negative number if [first] should be placed before [second] and
     * a positive number if [second] should be placed before [first], and 0 if another comparison should be used to
     * tiebreak.
     */
    abstract fun compare(first: T, firstIndex: Int, second: T, secondIndex: Int): Int

    /**
     * The content for the header of this column.
     */
    @Composable
    abstract fun header(sort: MutableState<Sort?>)

    /**
     * The content for the given [item] at the given [index].
     */
    @Composable
    abstract fun item(item: T, index: Int)

    /**
     * The standard table header with a text [header].
     */
    @Composable
    fun standardHeader(
        sort: MutableState<Sort?>,
        header: String,
        sortable: Boolean = true,
        padding: Dp = Dimens.space3
    ) {
        if (sortable) {
            SimpleTextButton(
                contentPadding = PaddingValues(end = padding),
                onClick = {
                    sort.value = when (sort.value) {
                        Sort.IN_ORDER -> Sort.REVERSE_ORDER
                        Sort.REVERSE_ORDER -> null
                        null -> Sort.IN_ORDER
                    }
                }
            ) {
                Text(
                    text = header,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = padding, top = padding, bottom = padding, end = padding / 2)
                )

                val icon = when (sort.value) {
                    Sort.IN_ORDER -> Icons.Default.KeyboardArrowUp
                    Sort.REVERSE_ORDER -> Icons.Default.KeyboardArrowDown
                    null -> Icons.Default.KeyboardArrowUp
                }

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.iconSmall),
                    tint = Colors.current.highlighted(sort.value != null, otherwise = Color.Transparent)
                )
            }
        } else {
            Text(text = header, fontWeight = FontWeight.Bold, modifier = Modifier.padding(padding))
        }
    }

    /**
     * Creates a new [Column] from this [Column] with the same values, but mapped with [mapper]. This is convenient for
     * reusing a [Column] with a different type of item but the same content.
     */
    fun <R> mapped(mapper: (R) -> T): Column<R> {
        val base = this
        return object : Column<R>() {
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
            override fun header(sort: MutableState<Sort?>) {
                base.header(sort)
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
                        val placeable = placeables[placeableIndex]
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
