package com.dzirbel.kotify.ui.page.library

import androidx.compose.runtime.Composable
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.table.ColumnByRelativeDateText

// TODO document
abstract class InvalidateButtonColumn<T>(name: String) : ColumnByRelativeDateText<T>(name = name) {
    @Composable
    override fun item(item: T, index: Int) {
        InvalidateButton(
            refreshing = isRefreshing(item, index),
            updated = timestampFor(item, index),
        ) {
            onInvalidate(item, index)
        }
    }

    abstract fun isRefreshing(item: T, index: Int): Boolean

    abstract fun onInvalidate(item: T, index: Int)
}
