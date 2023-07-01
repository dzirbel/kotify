package com.dzirbel.kotify.util

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

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
    return mapParallel(transform).flatten()
}
