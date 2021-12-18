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

/**
 * Compares this nullable [T] to [other], simulating the semantics of [compareTo] but allowing for sorting nulls. If
 * [nullsFirst] is true then null values are considered less than any non-null value; if it is false they are considered
 * greater.
 */
fun <T : Comparable<T>> T?.compareToNullable(other: T?, nullsFirst: Boolean = true): Int {
    return when {
        this != null && other != null -> this.compareTo(other)
        this != null -> if (nullsFirst) 1 else -1 // other is null
        other != null -> if (nullsFirst) -1 else 1 // this is null
        else -> 0 // both null -> equal
    }
}
