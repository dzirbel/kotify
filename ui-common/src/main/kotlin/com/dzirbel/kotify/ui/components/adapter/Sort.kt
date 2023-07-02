package com.dzirbel.kotify.ui.components.adapter

/**
 * Encapsulates a [sortableProperty] and a particular [sortOrder], which together provide a means to impose an ordering
 * on element of type [E]; namely as [comparator].
 */
data class Sort<E>(
    val sortableProperty: SortableProperty<E>,
    val sortOrder: SortOrder = sortableProperty.defaultSortOrder,
) {
    /**
     * A [Comparator] which compares elements of type [E] according to [sortableProperty] and [sortOrder].
     */
    val comparator: Comparator<E>
        get() = Comparator { o1, o2 -> sortableProperty.compare(sortOrder, o1, o2) }
}

/**
 * Coalesces this [List] of [Sort]s into a single [Comparator] which compares them in the order of the [Sort]s in the
 * [List], moving down the list to break ties.
 */
fun <E> List<Sort<E>>.asComparator(): Comparator<E> {
    return fold(Comparator { _, _ -> 0 }) { comparator, sort -> comparator.thenComparing(sort.comparator) }
}
