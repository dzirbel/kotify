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
interface InvalidateButtonColumn<T> : Column<T> {
    override val sortableProperty
        get() = object : SortableProperty<T> {
            override val title = this@InvalidateButtonColumn.title

            override fun compare(sortOrder: SortOrder, first: T, second: T): Int {
                return sortOrder.compareNullable(timestampFor(first), timestampFor(second))
            }
        }

    @Composable
    override fun item(item: T) {
        InvalidateButton(
            refreshing = isRefreshing(item),
            updated = timestampFor(item),
            onClick = { onInvalidate(item) },
        )
    }

    /**
     * The timestamp at which the resource was last updated, for the given [item].
     */
    fun timestampFor(item: T): Long?

    /**
     * Whether the given [item] is actively being refreshed.
     */
    fun isRefreshing(item: T): Boolean

    /**
     * Invoked when the [InvalidateButton] is clicked for the given [item].
     */
    fun onInvalidate(item: T)
}
