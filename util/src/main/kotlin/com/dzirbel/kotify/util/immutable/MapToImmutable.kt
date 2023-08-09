package com.dzirbel.kotify.util.immutable

import kotlinx.collections.immutable.ImmutableList

/**
 * Applies [mapper] to all the elements of this [List], producing an [ImmutableList].
 */
inline fun <E, reified F> Collection<E>.mapToImmutableList(mapper: (E) -> F): ImmutableList<F> {
    val iterator = this.iterator()
    return Array(size) { mapper(iterator.next()) }.toImmutableList()
}

inline fun <E, reified F> Collection<E>.mapIndexedToImmutableList(mapper: (IndexedValue<E>) -> F): ImmutableList<F> {
    val iterator = this.iterator().withIndex()
    return Array(size) { mapper(iterator.next()) }.toImmutableList()
}
