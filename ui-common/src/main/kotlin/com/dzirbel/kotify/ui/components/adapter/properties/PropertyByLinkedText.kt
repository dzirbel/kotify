package com.dzirbel.kotify.ui.components.adapter.properties

import com.dzirbel.kotify.ui.components.adapter.DividableProperty
import com.dzirbel.kotify.ui.components.adapter.SortOrder
import com.dzirbel.kotify.ui.components.adapter.SortableProperty
import com.dzirbel.kotify.ui.components.adapter.compare
import com.dzirbel.kotify.ui.components.adapter.compareNullable
import com.dzirbel.kotify.ui.components.table.ColumnByLinkedText

abstract class PropertyByLinkedText<E>(override val title: String) :
    SortableProperty<E>, DividableProperty<E>, ColumnByLinkedText<E> {

    override val terminalSort = true

    override fun compare(sortOrder: SortOrder, first: E, second: E): Int {
        return sortOrder.compare(
            first = links(first).joinToString(separator = "") { it.text },
            second = links(second).joinToString(separator = "") { it.text },
            ignoreCase = true,
        )
    }

    override fun divisionFor(element: E): Char? {
        return links(element).firstNotNullOfOrNull { link ->
            link.link.firstOrNull()?.let { firstChar ->
                if (firstChar.isLetter()) firstChar.uppercaseChar() else '#'
            }
        }
    }

    override fun compareDivisions(sortOrder: SortOrder, first: Any?, second: Any?): Int {
        return sortOrder.compareNullable(first as? Char, second as? Char)
    }
}
