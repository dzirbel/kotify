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
interface ColumnByString<T> : Column<T> {
    val cellPadding: Dp
        get() = Dimens.space3

    /**
     * Renders the content of the given [item] as the returned string.
     */
    fun toString(item: T): String?

    @Composable
    override fun item(item: T) {
        Text(text = toString(item).orEmpty(), modifier = Modifier.padding(cellPadding))
    }
}
