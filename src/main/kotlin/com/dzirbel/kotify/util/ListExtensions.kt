package com.dzirbel.kotify.util

/**
 * Returns a new [List] containing the elements of this [List] and the given [elements], inserted according to the
 * natural order of [selector].
 *
 * This [List] must be sorted according to [selector].
 */
fun <T, R : Comparable<R>> List<T>.plusSorted(elements: List<T>, selector: (T) -> R): List<T> {
    if (elements.isEmpty()) return this

    val result = ArrayList<T>(size + elements.size)
    result.addAll(this)

    for (element in elements) {
        var index = result.binarySearchBy(key = selector(element), selector = selector)
        if (index < 0) {
            index = -(index + 1)
        }
        result.add(index, element)
    }

    return result
}
