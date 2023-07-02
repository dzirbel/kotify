package com.dzirbel.kotify.ui.components.adapter.properties

import com.dzirbel.kotify.ui.components.adapter.DividableProperty
import com.dzirbel.kotify.ui.components.adapter.SortOrder
import com.dzirbel.kotify.ui.components.adapter.SortableProperty
import com.dzirbel.kotify.ui.components.adapter.compare
import com.dzirbel.kotify.ui.components.adapter.compareNullable
import com.dzirbel.kotify.ui.components.table.ColumnByNumber
import java.util.concurrent.TimeUnit

abstract class PropertyByNumber<E>(override val title: String, private val divisionRange: Int) :
    SortableProperty<E>, DividableProperty<E>, ColumnByNumber<E> {
    open val nullsFirst: Boolean = false

    override fun compare(sortOrder: SortOrder, first: E, second: E): Int {
        val firstNumber = toNumber(first)?.toDouble()
        val secondNumber = toNumber(second)?.toDouble()

        return sortOrder.compareNullable(firstNumber, secondNumber, nullsFirst = nullsFirst)
    }

    override fun divisionFor(element: E): Int {
        return toNumber(element)?.toInt()?.let { divisionRange * (it / divisionRange) } ?: 0
    }

    override fun divisionTitle(division: Any?): String? {
        val number = requireNotNull(division) as Int
        return "$number - ${number + divisionRange}"
    }

    override fun compareDivisions(sortOrder: SortOrder, first: Any?, second: Any?): Int {
        return sortOrder.compare(requireNotNull(first) as Int, requireNotNull(second) as Int)
    }

    companion object {
        const val POPULARITY_DIVISION_RANGE = 10
        const val TRACK_INDEX_DIVISION_RANGE = 10
        val DURATION_DIVISION_RANGE_MS = TimeUnit.SECONDS.toMillis(10).toInt()
    }
}
