package com.dzirbel.kotify.ui.components.adapter

/**
 * Encapsulates a [dividableProperty] and a particular [divisionSortOrder], which together provide a means to divide
 * elements of type [E] and impose an ordering on the resulting divisions; namely [divisionComparator].
 */
data class Divider<E>(
    val dividableProperty: DividableProperty<E>,
    val divisionSortOrder: SortOrder = dividableProperty.defaultDivisionSortOrder,
) {
    /**
     * A [Comparator] which compares divisions according to [dividableProperty] and [divisionSortOrder].
     */
    val divisionComparator: Comparator<Any?>
        get() = Comparator { o1, o2 -> dividableProperty.compareDivisions(divisionSortOrder, o1, o2) }
}
