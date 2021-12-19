package com.dzirbel.kotify.ui.components.table

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.ui.components.liveRelativeDateText
import com.dzirbel.kotify.ui.theme.Dimens

/**
 * A standard [Column] which displays a relative date text for each row based on [timestampFor].
 */
abstract class ColumnByRelativeDateText<T>(
    name: String,
    sortable: Boolean = true,
    private val padding: Dp = Dimens.space3,
) : Column<T>(name = name, sortable = sortable) {
    /**
     * Returns the timestamp of the content to be rendered as a relative date.
     */
    abstract fun timestampFor(item: T, index: Int): Long?

    override fun compare(first: T, firstIndex: Int, second: T, secondIndex: Int): Int {
        return (timestampFor(first, firstIndex) ?: 0).compareTo(timestampFor(second, secondIndex) ?: 0)
    }

    @Composable
    override fun header(sortOrder: SortOrder?, onSetSort: (SortOrder?) -> Unit) {
        standardHeader(sortOrder = sortOrder, onSetSort = onSetSort, padding = padding)
    }

    @Composable
    override fun item(item: T, index: Int) {
        val text = timestampFor(item, index)?.let { liveRelativeDateText(timestamp = it) }.orEmpty()
        Text(text = text, modifier = Modifier.padding(Dimens.space3))
    }
}
