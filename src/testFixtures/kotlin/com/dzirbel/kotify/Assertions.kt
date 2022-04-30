package com.dzirbel.kotify

import assertk.Assert
import assertk.assertThat
import assertk.assertions.containsAll
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.support.expected
import assertk.assertions.support.show
import java.util.SortedMap

/**
 * Wraps this value with in an [Assert] and invokes [assertion] on it, as a convenience to avoid calls to let{}.
 */
fun <T> T.assertThat(assertion: Assert<T>.() -> Unit) {
    assertThat(this).assertion()
}

/**
 * Asserts that the value is null if [shouldBeNull] is true or is non-null if it is false.
 */
fun <T : Any> Assert<T?>.isNullIf(shouldBeNull: Boolean) {
    if (shouldBeNull) isNull() else isNotNull()
}

/**
 * Asserts that the value is the same instance as [other].
 */
fun <T> Assert<T>.isSameInstanceAs(other: T) {
    given { actual ->
        if (actual === other) return

        if (actual == other) {
            expected("to be the same instance as equal-valued object:${show(other)}")
        } else {
            expected("to be the same instance as:${show(other)} but was not equal:${show(actual)}")
        }
    }
}

/**
 * Asserts that the value is not the same instance as [other].
 */
fun <T> Assert<T>.isNotSameInstanceAs(other: T) {
    given { actual ->
        if (actual !== other) return

        expected("not to be the same instance as:${show(other)}")
    }
}

/**
 * Asserts the [List] contains exactly the expected [elements] of the given [List]. They must be in the same order and
 * there must not be any extra elements.
 *
 * @see [containsExactly]
 */
inline fun <reified T> Assert<List<T>>.containsExactlyElementsOf(elements: List<T>) {
    @Suppress("SpreadOperator")
    containsExactly(*elements.toTypedArray())
}

/**
 * Asserts the [Iterable] contains exactly the expected [elements] of the given [Iterable], in any order. Each value in
 * expected must correspond to a matching value in actual, and visa-versa.
 *
 * @see [containsExactlyInAnyOrder]
 */
inline fun <reified T> Assert<Iterable<T>>.containsExactlyElementsOfInAnyOrder(elements: Iterable<T>) {
    @Suppress("SpreadOperator")
    containsExactlyInAnyOrder(*elements.toList().toTypedArray())
}

/**
 * Asserts the [Iterable] contains all the expected [elements] of the given [Iterable], in any order. The collection may
 * also contain additional elements.
 *
 * @see [containsAll]
 */
inline fun <reified T> Assert<Iterable<T>>.containsAllElementsOf(elements: Iterable<T>) {
    @Suppress("SpreadOperator")
    containsAll(*elements.toList().toTypedArray())
}

/**
 * Asserts the [SortedMap] contains exactly the expected [elements]. They must be in the same order as the [SortedMap]'s
 * iteration order and there must not be any extra elements.
 */
@Suppress("SpreadOperator")
fun <K, V> Assert<SortedMap<out K, out V>>.containsExactly(vararg elements: Pair<K, V>) {
    given { actual ->
        assertThat(actual.toList()).containsExactly(*elements)
    }
}
