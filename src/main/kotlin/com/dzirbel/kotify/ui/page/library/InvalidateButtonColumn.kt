package com.dzirbel.kotify.ui.page.library

import androidx.compose.runtime.Composable
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.adapter.SortOrder
import com.dzirbel.kotify.ui.components.adapter.SortableProperty
import com.dzirbel.kotify.ui.components.adapter.compareNullable
import com.dzirbel.kotify.ui.components.table.Column

/**
 * A [Column] which renders an [InvalidateButton] based on [timestampFor].
 */
abstract class InvalidateButtonColumn<T>(name: String) : Column<T>(name = name) {
    final override val sortableProperty = object : SortableProperty<T>(sortTitle = name) {
        override fun compare(sortOrder: SortOrder, first: IndexedValue<T>, second: IndexedValue<T>): Int {
            return sortOrder.compareNullable(
                timestampFor(first.value, first.index),
                timestampFor(second.value, second.index),
            )
        }
    }

    @Composable
    final override fun item(item: T, index: Int) {
        InvalidateButton(
            refreshing = isRefreshing(item, index),
            updated = timestampFor(item, index),
            onClick = { onInvalidate(item, index) },
        )
    }

    /**
     * The timestamp at which the resource was last updated, for the given [item].
     */
    abstract fun timestampFor(item: T, index: Int): Long?

    /**
     * Whether the given [item] is actively being refreshed.
     */
    abstract fun isRefreshing(item: T, index: Int): Boolean

    /**
     * Invoked when the [InvalidateButton] is clicked for the given [item].
     */
    abstract fun onInvalidate(item: T, index: Int)
}
