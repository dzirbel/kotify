package com.dzirbel.kotify.util.collections

/**
 * Sums the result of applying [map] to all the elements of this [Iterable], ignoring those for which [map] returns
 * null.
 */
fun <T> Iterable<T>.sumOf(map: (T) -> Float?): Float {
    var total = 0f
    for (element in this) {
        map(element)?.let { total += it }
    }
    return total
}
