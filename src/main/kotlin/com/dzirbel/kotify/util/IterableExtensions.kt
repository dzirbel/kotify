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

/**
 * Returns this [List] if all its elements are non-null, otherwise returns null.
 */
fun <T> List<T?>.takeIfAllNonNull(): List<T>? {
    if (all { it != null }) @Suppress("unchecked_cast") return this as List<T> else return null
}

/**
 * Returns a copy of this [List] with [value] added if [condition] is true or remove if it is false.
 */
fun <T> List<T>.plusOrMinus(value: T, condition: Boolean): List<T> = if (condition) plus(value) else minus(value)

/**
 * Returns a copy of this [Set] with [value] added if [condition] is true or remove if it is false.
 */
fun <T> Set<T>.plusOrMinus(value: T, condition: Boolean): Set<T> = if (condition) plus(value) else minus(value)
