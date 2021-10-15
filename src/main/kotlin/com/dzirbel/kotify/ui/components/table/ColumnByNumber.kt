package com.dzirbel.kotify.ui.components.table

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.ui.theme.Dimens

/**
 * A simple [Column] where the content is rendered as [Text] via [toNumber]. Used rather than [ColumnByString] to also
 * correctly sort by [toNumber].
 */
abstract class ColumnByNumber<T>(
    private val header: String,
    private val padding: Dp = Dimens.space3,
    private val sortable: Boolean = true,
) : Column<T>() {
    /**
     * Renders the content of the given [item] as the returned number.
     */
    abstract fun toNumber(item: T, index: Int): Number?

    override fun compare(first: T, firstIndex: Int, second: T, secondIndex: Int): Int {
        val firstNumber = toNumber(first, firstIndex)?.toDouble() ?: 0.0
        val secondNumber = toNumber(second, secondIndex)?.toDouble() ?: 0.0
        return firstNumber.compareTo(secondNumber)
    }

    @Composable
    override fun header(sort: MutableState<Sort?>) {
        standardHeader(sort = sort, header = header, padding = padding, sortable = sortable)
    }

    @Composable
    override fun item(item: T, index: Int) {
        Text(text = toNumber(item, index)?.toString().orEmpty(), modifier = Modifier.padding(padding))
    }
}
