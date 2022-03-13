package com.dzirbel.kotify.ui.components.adapter

import com.dzirbel.kotify.util.compareInOrder

data class Sort<T>(
    val sortableProperty: SortableProperty<T>,
    val sortOrder: SortOrder = sortableProperty.defaultOrder,
) {
    /**
     * Returns a [Comparator] which compares indexed elements of type [T] according to [SortableProperty.compare] and
     * [sortOrder].
     */
    val comparator: Comparator<IndexedValue<T>>
        get() = Comparator { o1, o2 -> sortableProperty.compare(sortOrder, o1, o2) }
}

fun <T> List<Sort<T>>.asComparator(): Comparator<IndexedValue<T>> {
    if (isEmpty()) {
        return Comparator { o1, o2 -> o1.index.compareTo(o2.index) }
    }

    return map { sort -> sort.comparator }.compareInOrder()
}
