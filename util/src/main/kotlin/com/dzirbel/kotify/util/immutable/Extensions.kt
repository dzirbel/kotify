package com.dzirbel.kotify.util.immutable

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf

/**
 * Returns a copy of this [PersistentSet] with [value] added if [condition] is true or removed if it is false.
 */
fun <T> PersistentSet<T>.plusOrMinus(value: T, condition: Boolean): PersistentSet<T> {
    return if (condition) add(value) else remove(value)
}

/**
 * Applies [mapper] to all the elements of this [List], producing an [ImmutableList].
 */
inline fun <E, reified F> Collection<E>.mapToImmutableList(mapper: (E) -> F): ImmutableList<F> {
    val array = arrayOfNulls<F>(size)

    var index = 0
    for (element in this) {
        array[index++] = mapper(element)
    }

    @Suppress("UNCHECKED_CAST")
    return (array as Array<F>).toImmutableList()
}

inline fun <E, reified F> Collection<E>.mapIndexedToImmutableList(mapper: (Int, E) -> F): ImmutableList<F> {
    val array = arrayOfNulls<F>(size)

    var index = 0
    for (element in this) {
        array[index++] = mapper(index, element)
    }

    @Suppress("UNCHECKED_CAST")
    return (array as Array<F>).toImmutableList()
}

/**
 * Creates an [ImmutableList] wrapping this [Array].
 */
fun <E> Array<E>.toImmutableList(): ImmutableList<E> = ImmutableArray(this)

fun <E> ImmutableList<E>?.orEmpty(): ImmutableList<E> {
    return this ?: persistentListOf()
}

fun <E> PersistentList<E>?.orEmpty(): PersistentList<E> {
    return this ?: persistentListOf()
}
