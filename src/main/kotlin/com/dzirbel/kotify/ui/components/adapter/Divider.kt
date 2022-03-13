package com.dzirbel.kotify.ui.components.adapter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.ui.components.HorizontalDivider
import com.dzirbel.kotify.ui.theme.Dimens

abstract class Divider<E>(
    val dividerTitle: String,
    val defaultDivisionSortOrder: SortOrder = SortOrder.ASCENDING,
) {
    fun divisionComparator(sortOrder: SortOrder): Comparator<Any> {
        return Comparator { o1, o2 -> compareDivisions(sortOrder, o1, o2) }
    }

    abstract fun compareDivisions(sortOrder: SortOrder, first: Any, second: Any): Int

    abstract fun divisionFor(element: E): Any

    @Composable
    open fun headerContent(division: Any) {
        standardHeaderContent(divisionHeader = division.toString())
    }

    @Composable
    fun standardHeaderContent(divisionHeader: String?) {
        divisionHeader?.let {
            Box {
                Text(
                    text = divisionHeader,
                    style = MaterialTheme.typography.h5,
                    modifier = Modifier.padding(horizontal = Dimens.space4, vertical = Dimens.space2),
                )

                HorizontalDivider(modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        return (other as? Divider<*>)?.dividerTitle?.equals(dividerTitle) == true
    }

    override fun hashCode(): Int {
        return dividerTitle.hashCode()
    }
}
