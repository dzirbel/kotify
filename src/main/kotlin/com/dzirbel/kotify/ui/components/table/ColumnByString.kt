package com.dzirbel.kotify.ui.components.table

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.ui.theme.Dimens

/**
 * A simple [Column] where the header and content are rendered as [Text] via [toString].
 */
abstract class ColumnByString<T>(
    private val header: String,
    private val padding: Dp = Dimens.space3,
    private val sortable: Boolean = true,
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
