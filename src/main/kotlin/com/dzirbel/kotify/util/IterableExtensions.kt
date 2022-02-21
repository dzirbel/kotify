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
 * Associates values in this [Iterable] by the key returned by [keySelector], concatenating values with the same key
 * into a [List] (preserving order in the source [Iterable]).
 */
fun <T, K> Iterable<T>.associateByCombiningIntoList(keySelector: (T) -> K): Map<K, List<T>> {
    val map = mutableMapOf<K, MutableList<T>>()
    for (element in this) {
        val key = keySelector(element)
        map[key]?.add(element)
            ?: run { map[key] = mutableListOf(element) }
    }
    return map
}

/**
 * Returns a copy of this [Set] with [value] added if [condition] is true or remove if it is false.
 */
fun <T> Set<T>.plusOrMinus(value: T, condition: Boolean): Set<T> = if (condition) plus(value) else minus(value)

/**
 * Returns a copy of this [Set] with [elements] added if [condition] is true or remove if it is false.
 */
fun <T> Set<T>.plusOrMinus(elements: Iterable<T>, condition: Boolean): Set<T> {
    return if (condition) plus(elements) else minus(elements.toSet())
}
