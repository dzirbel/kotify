package com.dzirbel.kotify.ui.components.sort

import com.dzirbel.kotify.util.compareInOrder

data class Sort<T>(val sortableProperty: SortableProperty<T>, val sortOrder: SortOrder) {
    /**
     * Returns a [Comparator] which compares indexed elements of type [T] according to [SortableProperty.compare] and
     * [sortOrder].
     */
    val comparator: Comparator<IndexedValue<T>>
        get() {
            return Comparator(sortableProperty::compare)
                .let { if (sortOrder == SortOrder.DESCENDING) it.reversed() else it }
        }
}

fun <T> Iterable<T>.sortedBy(sorts: List<Sort<T>>): List<T> {
    // TODO avoid mapping to remove index
    return withIndex().sortedWith(sorts.asComparator()).map { it.value }
}

fun <T> List<Sort<T>>.asComparator(): Comparator<IndexedValue<T>> {
    if (isEmpty()) {
        return Comparator { o1, o2 -> o1.index.compareTo(o2.index) }
    }

    return map { sort -> sort.comparator }
        .compareInOrder()
        .thenComparing { o1, o2 -> o1.index.compareTo(o2.index) }
}
