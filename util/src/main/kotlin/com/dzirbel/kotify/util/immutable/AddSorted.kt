package com.dzirbel.kotify.util.immutable

import com.dzirbel.kotify.util.collections.sortedIndexFor
import kotlinx.collections.immutable.PersistentList

/**
 * Returns a new [PersistentList] with [element] added at the index where it would be sorted according to [comparator].
 */
fun <E> PersistentList<E>.addSorted(element: E, comparator: Comparator<E>): PersistentList<E> {
    return add(index = sortedIndexFor(element, comparator), element = element)
}
