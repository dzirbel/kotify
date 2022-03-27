package com.dzirbel.kotify.ui.components.table

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.ui.theme.Dimens

/**
 * A simple [Column] where the content is rendered as [Text] via [toNumber]. Used rather than [ColumnByString] to also
 * correctly sort by [toNumber].
 */
interface ColumnByNumber<T> : Column<T> {
    val cellPadding: Dp
        get() = Dimens.space3

    /**
     * Renders the content of the given [item] as the returned number.
     */
    fun toNumber(item: T): Number?

    fun toString(item: T): String = toNumber(item)?.toString().orEmpty()

    @Composable
    override fun item(item: T) {
        Text(text = toString(item), modifier = Modifier.padding(cellPadding))
    }
}
