package com.dzirbel.kotify.util.coroutines

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

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
