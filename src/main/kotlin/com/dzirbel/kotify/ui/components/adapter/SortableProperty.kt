package com.dzirbel.kotify.ui.components.adapter

/**
 * Represents an abstract property of a collection of data (e.g. table or grid) which specifies how its elements of type
 * [E] can be sorted.
 */
abstract class SortableProperty<E>(
    val sortTitle: String,
    val defaultOrder: SortOrder = SortOrder.ASCENDING,

    /**
     * Whether this property imparts an unambiguous order, with no two elements being equal (unless they are the same
     * element). In this case the user will not be allowed to refine the sort further by adding tie-breaking sorts
     * beyond it.
     */
    val terminal: Boolean = false,
) {
    abstract fun compare(sortOrder: SortOrder, first: IndexedValue<E>, second: IndexedValue<E>): Int
}
