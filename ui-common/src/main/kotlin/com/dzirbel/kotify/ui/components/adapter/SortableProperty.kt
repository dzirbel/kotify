package com.dzirbel.kotify.ui.components.adapter

import androidx.compose.runtime.Immutable

/**
 * Represents a property by which objects of type [E] can be sorted.
 *
 * Note that [SortableProperty] extends [Comparator] whose functions can be used to compare elements of type [E].
 */
@Immutable
interface SortableProperty<E> : AdapterProperty<E> {
    /**
     * A user-readable name of this property, specific to its use in sorting. By default uses [title] but may be
     * overridden to use a specific name for sorting.
     */
    val sortTitle: String
        get() = title

    /**
     * The default [SortOrder] in which orderings of objects should be oriented.
     */
    val defaultSortOrder: SortOrder
        get() = SortOrder.ASCENDING

    /**
     * Whether this property imparts an unambiguous order, with no two elements being equal (unless they are the same
     * element). In this case the user will not be allowed to refine the sort further by adding tie-breaking sorts
     * beyond it.
     */
    val terminalSort: Boolean
        get() = false

    /**
     * Compares elements [first] and [second], returning a positive integer if [first] is greater than [second], a
     * negative integer if [first] is less than [second], or zero if they are equal.
     *
     * Note that [sortOrder] must be provided here in order to allow comparisons which are not invertable, e.g.
     * comparisons which put null values last whether in ascending or descending order.
     */
    fun compare(sortOrder: SortOrder, first: E, second: E): Int
}
