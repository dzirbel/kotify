package com.dzirbel.kotify.util

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
