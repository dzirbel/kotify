package com.dzirbel.kotify.util

/**
 * Maps this [Array] to a [List] via [transform], short-circuiting and returning null if any transformed values are
 * null.
 */
fun <T, R> Array<T>.mapIfAllNotNull(transform: (T) -> R?): List<R>? {
    val result = ArrayList<R>(this.size)
    for (element in this) {
        result.add(transform(element) ?: return null)
    }
    return result
}
