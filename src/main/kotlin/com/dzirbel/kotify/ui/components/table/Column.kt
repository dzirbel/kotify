package com.dzirbel.kotify.ui.components.table

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.components.sort.SortOrder
import com.dzirbel.kotify.ui.components.sort.SortableProperty
import com.dzirbel.kotify.ui.components.sort.icon
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors

/**
 * Represents a single column in a [Table].
 */
abstract class Column<T>(val name: String) {
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
     * Determines how elements should be sorted by this column, or null if the data cannot be sorted by this column.
     */
    open val sortableProperty: SortableProperty<T>? = null

    /**
     * The content for the header of this column.
     */
    @Composable
    open fun header(sortOrder: SortOrder?, onSetSort: (SortOrder?) -> Unit) {
        standardHeader(sortOrder = sortOrder, onSetSort = onSetSort)
    }

    /**
     * The content for the given [item] at the given [index].
     */
    @Composable
    abstract fun item(item: T, index: Int)

    /**
     * The standard table header with a text [header] and which may be sorted if [sortableProperty] is non-null.
     */
    @Composable
    fun standardHeader(
        sortOrder: SortOrder?,
        onSetSort: (SortOrder?) -> Unit,
        header: String = name,
        padding: Dp = Dimens.space3,
    ) {
        if (sortableProperty != null) {
            SimpleTextButton(
                contentPadding = PaddingValues(end = padding),
                onClick = {
                    onSetSort(
                        when (sortOrder) {
                            SortOrder.ASCENDING -> SortOrder.DESCENDING
                            SortOrder.DESCENDING -> null
                            null -> SortOrder.ASCENDING
                        }
                    )
                }
            ) {
                Text(
                    text = header,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = padding, top = padding, bottom = padding, end = padding / 2)
                )

                Icon(
                    imageVector = sortOrder.icon,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.iconSmall),
                    tint = LocalColors.current.highlighted(sortOrder != null, otherwise = Color.Transparent)
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
    fun <R> mapped(name: String = this.name, mapper: (R) -> T): Column<R> {
        val base = this
        return object : Column<R>(name = name) {
            override val width = base.width
            override val cellAlignment = base.cellAlignment
            override val headerAlignment = base.headerAlignment

            override val sortableProperty: SortableProperty<R>? = base.sortableProperty?.let { baseSortProperty ->
                object : SortableProperty<R>(sortTitle = name) {
                    override fun compare(first: IndexedValue<R>, second: IndexedValue<R>): Int {
                        return baseSortProperty.compare(
                            first = IndexedValue(index = first.index, value = mapper(first.value)),
                            second = IndexedValue(index = second.index, value = mapper(second.value)),
                        )
                    }
                }
            }

            @Composable
            override fun header(sortOrder: SortOrder?, onSetSort: (SortOrder?) -> Unit) {
                base.header(sortOrder, onSetSort)
            }

            @Composable
            override fun item(item: R, index: Int) {
                base.item(mapper(item), index)
            }
        }
    }
}
