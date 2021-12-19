package com.dzirbel.kotify.ui.components.table

/**
 * A standard [Column] which displays a 1-based index for each row.
 */
class IndexColumn<T> : ColumnByNumber<T>(name = "#", sortable = false) {
    override fun toNumber(item: T, index: Int) = index + 1
}
