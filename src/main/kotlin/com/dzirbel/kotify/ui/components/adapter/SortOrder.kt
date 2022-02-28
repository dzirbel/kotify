package com.dzirbel.kotify.ui.components.adapter

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.graphics.vector.ImageVector

enum class SortOrder { ASCENDING, DESCENDING }

val SortOrder.flipped: SortOrder
    get() {
        return when (this) {
            SortOrder.ASCENDING -> SortOrder.DESCENDING
            SortOrder.DESCENDING -> SortOrder.ASCENDING
        }
    }

val SortOrder?.icon: ImageVector
    get() {
        return when (this) {
            SortOrder.ASCENDING -> Icons.Default.KeyboardArrowUp
            SortOrder.DESCENDING -> Icons.Default.KeyboardArrowDown
            null -> Icons.Default.KeyboardArrowUp
        }
    }

fun <T : Comparable<T>> SortOrder.naturalOrder(): Comparator<T> {
    return when (this) {
        SortOrder.ASCENDING -> Comparator.naturalOrder()
        SortOrder.DESCENDING -> Comparator.reverseOrder()
    }
}
