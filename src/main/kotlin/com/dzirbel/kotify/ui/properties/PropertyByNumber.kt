package com.dzirbel.kotify.ui.properties

import com.dzirbel.kotify.ui.components.adapter.DividableProperty
import com.dzirbel.kotify.ui.components.adapter.SortOrder
import com.dzirbel.kotify.ui.components.adapter.SortableProperty
import com.dzirbel.kotify.ui.components.adapter.compare
import com.dzirbel.kotify.ui.components.adapter.compareNullable
import com.dzirbel.kotify.ui.components.table.ColumnByNumber

abstract class PropertyByNumber<E>(override val title: String) :
    SortableProperty<E>, DividableProperty<E>, ColumnByNumber<E> {
    open val nullsFirst: Boolean = false

    override fun compare(sortOrder: SortOrder, first: E, second: E): Int {
        val firstNumber = toNumber(first)?.toDouble()
        val secondNumber = toNumber(second)?.toDouble()

        return sortOrder.compareNullable(firstNumber, secondNumber, nullsFirst = nullsFirst)
    }

    @Suppress("UnnecessaryParentheses", "MagicNumber")
    override fun divisionFor(element: E): Int {
        // TODO allow more advanced numeric division
        return toNumber(element)?.toInt()?.let { (it / 10) * 10 } ?: 0
    }

    override fun compareDivisions(sortOrder: SortOrder, first: Any?, second: Any?): Int {
        return sortOrder.compare(first as Int, second as Int)
    }
}
