package com.dzirbel.kotify.ui.components.table

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.ui.components.LinkedText
import com.dzirbel.kotify.ui.components.adapter.SortOrder
import com.dzirbel.kotify.ui.components.adapter.SortableProperty
import com.dzirbel.kotify.ui.components.adapter.compare
import com.dzirbel.kotify.ui.theme.Dimens

abstract class ColumnByLinkedText<T>(name: String, private val padding: Dp = Dimens.space3) : Column<T>(name = name) {
    data class Link(val text: String, val link: String)

    override val sortableProperty = object : SortableProperty<T>(sortTitle = name) {
        override fun compare(sortOrder: SortOrder, first: IndexedValue<T>, second: IndexedValue<T>): Int {
            return sortOrder.compare(
                first = links(first.value, first.index).joinToString { it.text },
                second = links(second.value, second.index).joinToString { it.text },
                ignoreCase = true,
            )
        }
    }

    @Composable
    override fun header(sortOrder: SortOrder?, onSetSort: (SortOrder?) -> Unit) {
        standardHeader(sortOrder = sortOrder, onSetSort = onSetSort, padding = padding)
    }

    @Composable
    final override fun item(item: T, index: Int) {
        val links = links(item, index)
        if (links.isNotEmpty()) {
            LinkedText(
                modifier = Modifier.padding(padding),
                key = item,
                onClickLink = ::onClickLink,
            ) {
                list(links) {
                    link(text = it.text, link = it.link)
                }
            }
        }
    }

    /**
     * Renders the contents for [item] as a list of [Link]s.
     */
    abstract fun links(item: T, index: Int): List<Link>

    /**
     * Invoked when the user clicks one of the rendered links.
     */
    abstract fun onClickLink(link: String)
}
