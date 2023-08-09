package com.dzirbel.kotify.util.collections

/**
 * Calculates the mean value of the numeric values provided by [toDouble] among non-null values in this [Iterable], or
 * null if there are no such values.
 */
fun <T : Any> Iterable<T?>.averageBy(toDouble: (T) -> Double?): Double? {
    var total = 0.0
    var count = 0

    for (element in this) {
        element?.let(toDouble)?.let { double ->
            total += double
            count++
        }
    }

    return if (count == 0) null else total / count
}
