package com.dzirbel.kotify.util

import assertk.Assert
import assertk.all
import assertk.assertions.containsAll
import assertk.assertions.containsAtLeast
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.support.expected
import java.util.SortedMap

/**
 * Asserts that the value is null if [shouldBeNull] is true or is non-null if it is false.
 */
fun <T : Any> Assert<T?>.isNullIf(shouldBeNull: Boolean) {
    if (shouldBeNull) isNull() else isNotNull()
}

/**
 * Asserts the [List] contains exactly the expected [elements] of the given [List]. They must be in the same order and
 * there must not be any extra elements.
 *
 * @see [containsExactly]
 */
inline fun <reified T> Assert<List<T>>.containsExactlyElementsOf(elements: List<T>) {
    containsExactly(*elements.toTypedArray())
}

/**
 * Asserts the [Iterable] contains exactly the expected [elements] of the given [Iterable], in any order. Each value in
 * expected must correspond to a matching value in actual, and visa-versa.
 *
 * @see [containsExactlyInAnyOrder]
 */
inline fun <reified T> Assert<Iterable<T>>.containsExactlyElementsOfInAnyOrder(elements: Iterable<T>) {
    containsExactlyInAnyOrder(*elements.toList().toTypedArray())
}

/**
 * Asserts the [Iterable] contains all the expected [elements] of the given [Iterable], in any order. The collection may
 * also contain additional elements.
 *
 * @see [containsAll]
 */
inline fun <reified T> Assert<Iterable<T>>.containsAllElementsOf(elements: Iterable<T>) {
    containsAtLeast(*elements.toList().toTypedArray())
}

/**
 * Asserts that the [List] contains elements satisfying the given [assertions], in order, with the same size.
 */
inline fun <reified T> Assert<List<T>>.elementsSatisfy(vararg assertions: (Assert<T>) -> Unit) {
    all {
        hasSize(assertions.size)
        for ((index, assertion) in assertions.withIndex()) {
            index(index).all(assertion)
        }
    }
}

/**
 * Asserts the [List] is sorted according to the given [comparator].
 */
fun <T> Assert<List<T>>.isSorted(comparator: Comparator<T>) {
    given { actual ->
        if (actual.size <= 1) return

        for (i in 1 until actual.size) {
            if (comparator.compare(actual[i - 1], actual[i]) > 0) {
                expected("elements ${i - 1} and $i to be in order but was ${actual[i - 1]} and ${actual[i]}")
            }
        }
    }
}

/**
 * Asserts the [SortedMap] contains exactly the expected [elements]. They must be in the same order as the [SortedMap]'s
 * iteration order and there must not be any extra elements.
 */
fun <K, V> Assert<SortedMap<out K, out V>>.containsExactly(vararg elements: Pair<K, V>) {
    given { actual ->
        assertThat(actual.toList()).containsExactly(*elements)
    }
}
