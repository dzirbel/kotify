package com.dzirbel.kotify.util.immutable

import kotlinx.collections.immutable.ImmutableList

/**
 * Returns an [ImmutableList] with the given known [size] by invoking [elementForIndex] for each of the elements in the
 * list.
 */
inline fun <reified E> buildImmutableList(size: Int, elementForIndex: (Int) -> E): ImmutableList<E> {
    val array = arrayOfNulls<E>(size)

    repeat(size) { index ->
        array[index] = elementForIndex(index)
    }

    @Suppress("UNCHECKED_CAST")
    return ImmutableArray(array as Array<E>)
}
