package com.dzirbel.kotify.util

/**
 * Returns this [T] if it is less than or equal to [maximumValue] or if [maximumValue] is null, and [maximumValue]
 * otherwise.
 */
fun <T : Comparable<T>> T.coerceAtMostNullable(maximumValue: T?): T {
    return if (maximumValue == null) this else coerceAtMost(maximumValue)
}

/**
 * Returns this [T] if it is greater than or equal to [minimumValue] or if [minimumValue] is null, and [minimumValue]
 * otherwise.
 */
fun <T : Comparable<T>> T.coerceAtLeastNullable(minimumValue: T?): T {
    return if (minimumValue == null) this else coerceAtLeast(minimumValue)
}
