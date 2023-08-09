package com.dzirbel.kotify.util.collections

import kotlinx.collections.immutable.PersistentSet

/**
 * Returns a copy of this [Set] with [value] added if [condition] is true or removed if it is false.
 */
fun <T> Set<T>.plusOrMinus(value: T, condition: Boolean): Set<T> = if (condition) plus(value) else minus(value)

/**
 * Returns a copy of this [Set] with [elements] added if [condition] is true or removed if it is false.
 */
fun <T> Set<T>.plusOrMinus(elements: Iterable<T>, condition: Boolean): Set<T> {
    return if (condition) plus(elements) else minus(elements.toSet())
}

/**
 * Returns a copy of this [PersistentSet] with [value] added if [condition] is true or removed if it is false.
 */
fun <T> PersistentSet<T>.plusOrMinus(value: T, condition: Boolean): PersistentSet<T> {
    return if (condition) add(value) else remove(value)
}
