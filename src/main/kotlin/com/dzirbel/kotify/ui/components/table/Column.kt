package com.dzirbel.kotify.ui.components.table

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.theme.Colors
import com.dzirbel.kotify.ui.theme.Dimens

/**
 * Represents a single column in a [Table].
 */
abstract class Column<T> {
    /**
     * The policy by which the width of the column is measured.
     */
    open val width: ColumnWidth = ColumnWidth.Fill()

    /**
     * The way that items in this [Column] are aligned within their cell.
     */
    open val cellAlignment: Alignment = Alignment.TopStart

    /**
     * The way that the header of this [Column] is aligned with its cell.
     */
    open val headerAlignment: Alignment = Alignment.BottomStart

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
     * The standard table header with a text [header] and which may be sorted if [sortable] is true.
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
            override val cellAlignment = base.cellAlignment
            override val headerAlignment = base.headerAlignment

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
