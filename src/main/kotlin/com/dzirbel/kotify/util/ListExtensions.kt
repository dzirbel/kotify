package com.dzirbel.kotify.util

import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap

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

/**
 * Returns a new [List] containing the elements of this [List] except for the element at [index].
 */
fun <T> List<T>.minusAt(index: Int): List<T> {
    require(index in indices)

    val result = ArrayList<T>(size - 1)
    forEachIndexed { i, element ->
        if (i != index) {
            result.add(element)
        }
    }
    return result
}

/**
 * Returns an [ImmutableMap] from the results of [map] to the number of times they occur.
 */
fun <T, K> Iterable<T>.countsBy(map: (T) -> K): ImmutableMap<K, Int> {
    val counts = mutableMapOf<K, Int>()
    for (element in this) {
        counts.compute(map(element)) { _, count -> if (count == null) 1 else count + 1 }
    }
    return counts.toImmutableMap()
}
