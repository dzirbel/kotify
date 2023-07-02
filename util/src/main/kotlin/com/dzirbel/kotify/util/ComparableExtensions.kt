package com.dzirbel.kotify.util

/**
 * Returns this [T] if it is less than or equal to [maximumValue] or if [maximumValue] is null, and [maximumValue]
 * otherwise.
 */
fun <T : Comparable<T>> T.coerceAtMostNullable(maximumValue: T?): T {
    return maximumValue?.let { coerceAtMost(it) } ?: this
}

/**
 * Returns this [T] if it is greater than or equal to [minimumValue] or if [minimumValue] is null, and [minimumValue]
 * otherwise.
 */
fun <T : Comparable<T>> T.coerceAtLeastNullable(minimumValue: T?): T {
    return minimumValue?.let { coerceAtLeast(it) } ?: this
}
