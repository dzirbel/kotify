package com.dzirbel.kotify.ui.components.sort

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
