package com.dzirbel.kotify.ui.components.table

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.ui.components.adapter.SortOrder
import com.dzirbel.kotify.ui.components.adapter.SortableProperty
import com.dzirbel.kotify.ui.components.adapter.compareNullable
import com.dzirbel.kotify.ui.theme.Dimens

/**
 * A simple [Column] where the content is rendered as [Text] via [toNumber]. Used rather than [ColumnByString] to also
 * correctly sort by [toNumber].
 */
abstract class ColumnByNumber<T>(
    name: String,
    private val nullsFirst: Boolean = false,
    private val padding: Dp = Dimens.space3,
) : Column<T>(name = name) {
    override val sortableProperty: SortableProperty<T>? = object : SortableProperty<T>(sortTitle = name) {
        override fun compare(sortOrder: SortOrder, first: IndexedValue<T>, second: IndexedValue<T>): Int {
            val firstNumber = toNumber(first.value, first.index)?.toDouble()
            val secondNumber = toNumber(second.value, second.index)?.toDouble()
            return sortOrder.compareNullable(firstNumber, secondNumber, nullsFirst = nullsFirst)
        }
    }

    /**
     * Renders the content of the given [item] as the returned number.
     */
    abstract fun toNumber(item: T, index: Int): Number?

    @Composable
    override fun header(sortOrder: SortOrder?, onSetSort: (SortOrder?) -> Unit) {
        standardHeader(sortOrder = sortOrder, onSetSort = onSetSort, padding = padding)
    }

    @Composable
    override fun item(item: T, index: Int) {
        Text(text = toNumber(item, index)?.toString().orEmpty(), modifier = Modifier.padding(padding))
    }
}
