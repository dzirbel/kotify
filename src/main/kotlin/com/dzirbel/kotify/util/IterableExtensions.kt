package com.dzirbel.kotify.util

/**
 * Invokes [onEach] for each pair built from the elements of this [Iterable] and [other] with the same index, up to the
 * minimum index of the two collections.
 *
 * Like [Iterable.zip] but doesn't construct a list of the resulting zipped values.
 */
fun <A, B> Iterable<A>.zipEach(other: Iterable<B>, onEach: (A, B) -> Unit) {
    val first = iterator()
    val second = other.iterator()
    while (first.hasNext() && second.hasNext()) {
        onEach(first.next(), second.next())
    }
}

/**
 * Returns a map of the pair of this [Iterable] and [other], associating keys in this [Iterable] with values in [other].
 * The returned map only has values from up to the minimum of the two collection sizes.
 */
fun <A, B> Iterable<A>.zipToMap(other: Iterable<B>): Map<A, B> {
    val map = mutableMapOf<A, B>()
    zipEach(other) { a, b ->
        map[a] = b
    }
    return map
}

fun <T> Iterable<T>.sumOfNullable(map: (T) -> Float?): Float {
    var total = 0f
    for (element in this) {
        map(element)?.let { total += it }
    }
    return total
}

/**
 * Calculates the mean value among the numeric value provided by [toDouble] among non-null values in this [Iterable], or
 * null if there are no such values.
 */
fun <T : Any> Iterable<T?>.averageOrNull(toDouble: (T) -> Double): Double? {
    var total = 0.0
    var count = 0

    for (element in this) {
        if (element != null) {
            total += toDouble(element)
            count++
        }
    }

    return if (count == 0) null else total / count
}
