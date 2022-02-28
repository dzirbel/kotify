package com.dzirbel.kotify.ui.components.table

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.ui.components.adapter.SortOrder
import com.dzirbel.kotify.ui.components.adapter.SortableProperty
import com.dzirbel.kotify.ui.theme.Dimens

/**
 * A simple [Column] where the header and content are rendered as [Text] via [toString].
 */
abstract class ColumnByString<T>(name: String, private val padding: Dp = Dimens.space3) : Column<T>(name = name) {
    override val sortableProperty = object : SortableProperty<T>(sortTitle = name) {
        override fun compare(first: IndexedValue<T>, second: IndexedValue<T>): Int {
            return toString(first.value, first.index).compareTo(toString(second.value, second.index), ignoreCase = true)
        }
    }

    /**
     * Renders the content of the given [item] as the returned string.
     */
    abstract fun toString(item: T, index: Int): String

    @Composable
    override fun header(sortOrder: SortOrder?, onSetSort: (SortOrder?) -> Unit) {
        standardHeader(sortOrder = sortOrder, onSetSort = onSetSort, padding = padding)
    }

    @Composable
    override fun item(item: T, index: Int) {
        Text(text = toString(item, index), modifier = Modifier.padding(padding))
    }
}
