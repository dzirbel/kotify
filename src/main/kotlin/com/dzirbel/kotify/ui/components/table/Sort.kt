package com.dzirbel.kotify.ui.components.table

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.graphics.vector.ImageVector

enum class SortOrder { ASCENDING, DESCENDING }

val SortOrder?.icon: ImageVector
    get() {
        return when (this) {
            SortOrder.ASCENDING -> Icons.Default.KeyboardArrowUp
            SortOrder.DESCENDING -> Icons.Default.KeyboardArrowDown
            null -> Icons.Default.KeyboardArrowUp
        }
    }

data class Sort<T>(val column: Column<T>, val sortOrder: SortOrder) {
    /**
     * Returns a [Comparator] which compares indexed elements of type [T] according to [Column.compare] and [sortOrder].
     */
    val comparator: Comparator<IndexedValue<T>>
        get() {
            return Comparator<IndexedValue<T>> { (firstIndex, first), (secondIndex, second) ->
                column.compare(
                    first = first,
                    firstIndex = firstIndex,
                    second = second,
                    secondIndex = secondIndex
                )
            }.let { if (sortOrder == SortOrder.DESCENDING) it.reversed() else it }
        }
}
