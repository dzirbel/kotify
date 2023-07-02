package com.dzirbel.kotify.ui.components.adapter.properties

import com.dzirbel.kotify.ui.components.adapter.DividableProperty
import com.dzirbel.kotify.ui.components.adapter.SortOrder
import com.dzirbel.kotify.ui.components.adapter.SortableProperty
import com.dzirbel.kotify.ui.components.adapter.compareNullable
import com.dzirbel.kotify.ui.components.table.ColumnByString

abstract class PropertyByString<E>(override val title: String) :
    SortableProperty<E>, DividableProperty<E>, ColumnByString<E> {

    override val terminalSort = true

    override fun compare(sortOrder: SortOrder, first: E, second: E): Int {
        return sortOrder.compareNullable(toString(first), toString(second))
    }

    override fun divisionFor(element: E): Char? {
        return toString(element)?.firstOrNull()?.let { firstChar ->
            if (firstChar.isLetter()) firstChar.uppercaseChar() else '#'
        }
    }

    override fun compareDivisions(sortOrder: SortOrder, first: Any?, second: Any?): Int {
        return sortOrder.compareNullable(first as? Char, second as? Char)
    }
}
