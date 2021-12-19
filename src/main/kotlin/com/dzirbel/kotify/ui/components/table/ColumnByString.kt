package com.dzirbel.kotify.ui.components.table

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.ui.theme.Dimens

/**
 * A simple [Column] where the header and content are rendered as [Text] via [toString].
 */
abstract class ColumnByString<T>(
    name: String,
    sortable: Boolean = true,
    private val padding: Dp = Dimens.space3,
) : Column<T>(name = name, sortable = sortable) {
    /**
     * Renders the content of the given [item] as the returned string.
     */
    abstract fun toString(item: T, index: Int): String

    override fun compare(first: T, firstIndex: Int, second: T, secondIndex: Int): Int {
        return toString(first, firstIndex).compareTo(toString(second, secondIndex), ignoreCase = true)
    }

    @Composable
    override fun header(sortOrder: SortOrder?, onSetSort: (SortOrder?) -> Unit) {
        standardHeader(sortOrder = sortOrder, onSetSort = onSetSort, padding = padding)
    }

    @Composable
    override fun item(item: T, index: Int) {
        Text(text = toString(item, index), modifier = Modifier.padding(padding))
    }
}
