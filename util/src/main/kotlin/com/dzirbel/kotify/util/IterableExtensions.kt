package com.dzirbel.kotify.util

import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Invokes [onEach] for each pair built from the elements of this [Iterable] and [other] with the same index, up to the
 * minimum index of the two collections.
 *
 * Like [Iterable.zip] but doesn't construct a list of the resulting zipped values.
 */
inline fun <A, B> Iterable<A>.zipEach(other: Iterable<B>, onEach: (A, B) -> Unit) {
    val first = iterator()
    val second = other.iterator()
    while (first.hasNext() && second.hasNext()) {
        onEach(first.next(), second.next())
    }
}

/**
 * Returns a [Map] of the pair of this [Iterable] and [other], associating keys in this [Iterable] with values in
 * [other]. The returned map only has values from up to the minimum of the two collection sizes.
 */
fun <A, B> Iterable<A>.zipToMap(other: Iterable<B>): Map<A, B> {
    val map = mutableMapOf<A, B>()
    zipEach(other) { a, b ->
        map[a] = b
    }
    return map
}

/**
 * Returns a [PersistentMap] of the pair of this [Iterable] and [other], associating keys in this [Iterable] with values
 * in [other]. The returned map only has values from up to the minimum of the two collection sizes.
 */
fun <A, B> Iterable<A>.zipToPersistentMap(other: Iterable<B>): PersistentMap<A, B> {
    return zipToMap(other).toPersistentMap()
}

/**
 * Sums the result of applying [map] to all the elements of this [Iterable], ignoring those for which [map] returns
 * null.
 */
fun <T> Iterable<T>.sumOfNullable(map: (T) -> Float?): Float {
    var total = 0f
    for (element in this) {
        map(element)?.let { total += it }
    }
    return total
}

/**
 * Calculates the mean value of the numeric values provided by [toDouble] among non-null values in this [Iterable], or
 * null if there are no such values.
 */
fun <T : Any> Iterable<T?>.averageOrNull(toDouble: (T) -> Double?): Double? {
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

/**
 * Maps values in this [Iterable] via [transform], computing each transformation in parallel.
 */
suspend fun <T, R> Iterable<T>.mapParallel(transform: suspend (T) -> R): List<R> {
    return coroutineScope {
        map { element ->
            async { transform(element) }
        }
    }
        .map { it.await() }
}

/**
 * Flat maps values in this [Iterable] via [transform], computing each transformation in parallel.
 */
suspend fun <T, R> Iterable<T>.flatMapParallel(transform: suspend (T) -> List<R>): List<R> {
    return coroutineScope {
        map { element ->
            async { transform(element) }
        }
    }
        .flatMap { it.await() }
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
