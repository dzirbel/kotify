package com.dzirbel.kotify.ui.properties

import com.dzirbel.kotify.network.model.ReleaseDate
import com.dzirbel.kotify.ui.components.adapter.DividableProperty
import com.dzirbel.kotify.ui.components.adapter.SortOrder
import com.dzirbel.kotify.ui.components.adapter.SortableProperty
import com.dzirbel.kotify.ui.components.adapter.compareNullable
import com.dzirbel.kotify.ui.components.table.ColumnByString

abstract class PropertyByReleaseDate<E>(override val title: String) :
    SortableProperty<E>, DividableProperty<E>, ColumnByString<E> {

    override val defaultSortOrder: SortOrder
        get() = SortOrder.DESCENDING

    override val defaultDivisionSortOrder: SortOrder
        get() = SortOrder.DESCENDING

    abstract fun releaseDateOf(item: E): ReleaseDate?

    override fun toString(item: E) = releaseDateOf(item)?.toString()

    override fun compare(sortOrder: SortOrder, first: E, second: E): Int {
        return sortOrder.compareNullable(releaseDateOf(first), releaseDateOf(second))
    }

    override fun divisionFor(element: E): Int? = releaseDateOf(element)?.year

    override fun compareDivisions(sortOrder: SortOrder, first: Any?, second: Any?): Int {
        return sortOrder.compareNullable(first as? Int, second as? Int)
    }
}
