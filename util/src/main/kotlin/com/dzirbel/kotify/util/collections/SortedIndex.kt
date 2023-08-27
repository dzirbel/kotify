package com.dzirbel.kotify.util.collections

/**
 * Returns the index at which [element] should be inserted into this list to maintain sorted order, using [comparator].
 *
 * Based on [binarySearch], but adds optimizations for short lists and elements before the first and after the last
 * element.
 *
 * Behavior is undefined if this list is not sorted according to [comparator].
 */
fun <E> List<E>.sortedIndexFor(element: E, comparator: Comparator<E>): Int {
    @Suppress("BracesOnWhenStatements")
    return when {
        isEmpty() -> 0
        size == 1 -> if (comparator.compare(element, this[0]) < 0) 0 else 1
        comparator.compare(element, this[lastIndex]) > 0 -> size
        comparator.compare(element, this[0]) < 0 -> 0
        size == 2 -> 1 // if size is 2 and not before first or after last, must be in the middle
        else -> {
            // use to/from index to avoid comparisons against the first and last elements, since we already checked them
            val index = binarySearch(element = element, comparator = comparator, fromIndex = 1, toIndex = size - 1)
            if (index < 0) -index - 1 else index
        }
    }
}
