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
import com.dzirbel.kotify.ui.components.adapter.AdapterProperty
import com.dzirbel.kotify.ui.components.adapter.SortOrder
import com.dzirbel.kotify.ui.components.adapter.SortableProperty
import com.dzirbel.kotify.ui.components.adapter.icon
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors

/**
 * Represents a single column in a [Table].
 */
interface Column<E> : AdapterProperty<E> {
    val columnTitle: String
        get() = title

    val headerPadding: Dp
        get() = Dimens.space3

    /**
     * The policy by which the width of the column is measured.
     */
    val width: ColumnWidth
        get() = ColumnWidth.Fill()

    /**
     * The way that items in this [Column] are aligned within their cell.
     */
    val cellAlignment: Alignment
        get() = Alignment.TopStart

    /**
     * The way that the header of this [Column] is aligned with its cell.
     */
    val headerAlignment: Alignment
        get() = Alignment.BottomStart

    /**
     * Determines how elements should be sorted by this column, or null if the data cannot be sorted by this column.
     */
    @Suppress("UNCHECKED_CAST")
    val sortableProperty: SortableProperty<E>?
        get() = this as? SortableProperty<E>

    /**
     * The content for the header of this column.
     */
    @Composable
    fun Header(sortOrder: SortOrder?, onSetSort: (SortOrder?) -> Unit) {
        if (sortableProperty != null) {
            SimpleTextButton(
                contentPadding = PaddingValues(end = headerPadding),
                onClick = {
                    onSetSort(
                        when (sortOrder) {
                            SortOrder.ASCENDING -> SortOrder.DESCENDING
                            SortOrder.DESCENDING -> null
                            null -> SortOrder.ASCENDING
                        },
                    )
                },
            ) {
                Text(
                    text = columnTitle,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        start = headerPadding,
                        top = headerPadding,
                        bottom = headerPadding,
                        end = headerPadding / 2,
                    ),
                )

                Icon(
                    imageVector = sortOrder.icon,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.iconSmall),
                    tint = LocalColors.current.highlighted(sortOrder != null, otherwise = Color.Transparent),
                )
            }
        } else {
            Text(text = columnTitle, fontWeight = FontWeight.Bold, modifier = Modifier.padding(headerPadding))
        }
    }

    /**
     * The cell content for the given [item].
     */
    @Composable
    fun Item(item: E)
}
