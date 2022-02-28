package com.dzirbel.kotify.ui.components.grid

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.dzirbel.kotify.ui.components.adapter.SortOrder
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.util.associateByCombiningIntoList

interface GridDivider<E> {
    val dividerTitle: String
    val sortOrder: SortOrder

    val divisionComparator: Comparator<String>
        get() = Comparator.naturalOrder<String>()
            .let {
                when (sortOrder) {
                    SortOrder.ASCENDING -> it
                    SortOrder.DESCENDING -> it.reversed()
                }
            }

    @Composable
    fun headerContent(division: GridDivision<E>) {
        standardHeaderContent(divisionHeader = division.header)
    }

    @Composable
    fun standardHeaderContent(divisionHeader: String?) {
        divisionHeader?.let {
            // TODO add underline
            Text(
                text = divisionHeader,
                fontSize = Dimens.fontTitle,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = Dimens.space4, vertical = Dimens.space2),
            )
        }
    }

    // retains order of elements within each division
    fun divide(elements: List<E>): List<GridDivision<E>>

    fun withSortOrder(sortOrder: SortOrder): GridDivider<E>
}

abstract class SimpleGridDivider<E>(override val dividerTitle: String) : GridDivider<E> {
    abstract fun divisionFor(element: E): String

    final override fun divide(elements: List<E>): List<GridDivision<E>> {
        val map = elements.associateByCombiningIntoList(::divisionFor)
        val comparator = Comparator.naturalOrder<String>()
            .let {
                when (sortOrder) {
                    SortOrder.ASCENDING -> it
                    SortOrder.DESCENDING -> it.reversed()
                }
            }

        return map.keys
            .sortedWith(comparator)
            .map { key -> GridDivision(header = key, elements = map.getValue(key)) }
    }
}
