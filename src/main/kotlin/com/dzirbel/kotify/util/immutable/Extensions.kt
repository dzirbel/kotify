package com.dzirbel.kotify.util.immutable

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentSet

/**
 * Returns a copy of this [PersistentSet] with [value] added if [condition] is true or removed if it is false.
 */
fun <T> PersistentSet<T>.plusOrMinus(value: T, condition: Boolean): PersistentSet<T> {
    return if (condition) add(value) else remove(value)
}

/**
 * Applies [mapper] to all the elements of this [List], producing an [ImmutableList].
 */
inline fun <E, reified F> List<E>.mapToImmutableList(mapper: (E) -> F): ImmutableList<F> {
    return buildImmutableList(size) { i -> mapper(this[i]) }
}
