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

/**
 * Returns the comparison of [first] and [second] relative to this [SortOrder] (i.e. their natural order if it is
 * [SortOrder.ASCENDING] or their reversed order if it is [SortOrder.DESCENDING]).
 */
fun <T : Comparable<T>> SortOrder.compare(first: T, second: T): Int {
    val comparison = first.compareTo(second)
    return when (this) {
        SortOrder.ASCENDING -> comparison
        SortOrder.DESCENDING -> -comparison
    }
}

/**
 * Returns the comparison of [first] and [second] relative to this [SortOrder], using [extractor] to extract the
 * [Comparable] value from each.
 */
fun <B, T : Comparable<T>> SortOrder.compareBy(first: B, second: B, extractor: (B) -> T): Int {
    return compare(extractor(first), extractor(second))
}

/**
 * Returns the comparison of [first] and [second] relative to this [SortOrder], optionally ignoring case per
 * [ignoreCase].
 */
fun SortOrder.compare(first: String, second: String, ignoreCase: Boolean = false): Int {
    val comparison = first.compareTo(second, ignoreCase = ignoreCase)
    return when (this) {
        SortOrder.ASCENDING -> comparison
        SortOrder.DESCENDING -> -comparison
    }
}

/**
 * Returns the comparison of [first] and [second] relative to this [SortOrder] for non-null elements, and placing nulls
 * either first or last depending on [nullsFirst].
 */
fun <T : Comparable<T>> SortOrder.compareNullable(first: T?, second: T?, nullsFirst: Boolean = false): Int {
    return when {
        first != null && second != null -> compare(first, second)
        first != null -> if (nullsFirst) 1 else -1 // other is null
        second != null -> if (nullsFirst) -1 else 1 // this is null
        else -> 0 // both null -> equal
    }
}

/**
 * Returns the comparison of [first] and [second] relative to this [SortOrder] for non-null elements, using [extractor]
 * to extract the [Comparable] value from each, and placing nulls either first or last depending on [nullsFirst].
 */
fun <B, T : Comparable<T>> SortOrder.compareByNullable(
    first: B?,
    second: B?,
    nullsFirst: Boolean = false,
    extractor: (B) -> T?,
): Int {
    return compareNullable(first = first?.let(extractor), second = second?.let(extractor), nullsFirst = nullsFirst)
}
