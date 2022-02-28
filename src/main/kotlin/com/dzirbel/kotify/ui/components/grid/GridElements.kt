package com.dzirbel.kotify.ui.components.grid

import com.dzirbel.kotify.ui.components.adapter.Sort
import com.dzirbel.kotify.ui.components.adapter.sortedBy

sealed class GridElements<T> {
    abstract val elements: List<T>
    abstract val divisions: List<GridDivision<T>>

    abstract fun withElements(elements: List<T>): GridElements<T>
    abstract fun sortedBy(sorts: List<Sort<T>>): GridElements<T>

    fun withDivider(divider: GridDivider<T>?): GridElements<T> {
        return if (divider == null) {
            PlainList(elements = elements)
        } else {
            DividedList(
                elements = elements,
                divisions = divider.divide(elements),
                divider = divider,
            )
        }
    }

    data class PlainList<T>(override val elements: List<T>) : GridElements<T>() {
        override val divisions: List<GridDivision<T>>
            get() = listOf(GridDivision(header = null, elements = elements))

        override fun withElements(elements: List<T>) = PlainList(elements = elements)

        override fun sortedBy(sorts: List<Sort<T>>): GridElements<T> = PlainList(elements = elements.sortedBy(sorts))
    }

    data class DividedList<T>(
        override val elements: List<T>,
        override val divisions: List<GridDivision<T>>,
        val divider: GridDivider<T>,
    ) : GridElements<T>() {
        override fun withElements(elements: List<T>): DividedList<T> {
            return copy(elements = elements, divisions = divider.divide(elements))
        }

        override fun sortedBy(sorts: List<Sort<T>>): GridElements<T> {
            val sorted = elements.sortedBy(sorts)
            return copy(elements = sorted, divisions = divider.divide(sorted))
        }

        companion object {
            fun <T> fromList(
                elements: List<T>,
                divider: GridDivider<T>,
                sorts: List<Sort<T>>? = null,
            ): DividedList<T> {
                val sortedList = if (sorts == null) elements else elements.sortedBy(sorts)
                val divisions = divider.divide(sortedList)
                return DividedList(
                    elements = sortedList,
                    divisions = divisions,
                    divider = divider,
                )
            }
        }
    }
}
