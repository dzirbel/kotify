package com.dzirbel.kotify.ui.components.adapter

import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.ui.theme.Dimens

abstract class Divider<E>(
    val dividerTitle: String,
    val divisionSortOrder: SortOrder = SortOrder.ASCENDING,
) {
    val divisionComparator: Comparator<String>
        get() = divisionSortOrder.naturalOrder()

    abstract fun divisionFor(element: E): String

    abstract fun withDivisionSortOrder(sortOrder: SortOrder): Divider<E>

    @Composable
    open fun headerContent(division: String) {
        standardHeaderContent(divisionHeader = division)
    }

    @Composable
    fun standardHeaderContent(divisionHeader: String?) {
        divisionHeader?.let {
            // TODO add underline
            Text(
                text = divisionHeader,
                style = MaterialTheme.typography.h5,
                modifier = Modifier.padding(horizontal = Dimens.space4, vertical = Dimens.space2),
            )
        }
    }
}
