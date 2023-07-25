package com.dzirbel.kotify.util

import assertk.Assert
import assertk.assertions.containsAll
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isNotNull
import assertk.assertions.isNull
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
    containsAll(*elements.toList().toTypedArray())
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
