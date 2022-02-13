package com.dzirbel.kotify.ui.components.table

import com.dzirbel.kotify.ui.components.sort.SortableProperty

/**
 * A standard [Column] which displays a 1-based index for each row.
 */
class IndexColumn<T> : ColumnByNumber<T>(name = "#") {
    // disable sorting by the index column since it is the default order
    override val sortableProperty: SortableProperty<T>? = null

    override fun toNumber(item: T, index: Int) = index + 1
}
