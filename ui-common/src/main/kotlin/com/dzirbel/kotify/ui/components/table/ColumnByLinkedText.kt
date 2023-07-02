package com.dzirbel.kotify.ui.components.table

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.ui.components.LinkedText
import com.dzirbel.kotify.ui.theme.Dimens

interface ColumnByLinkedText<T> : Column<T> {
    val cellPadding: Dp
        get() = Dimens.space3

    data class Link(val text: String, val link: String)

    @Composable
    override fun Item(item: T) {
        val links = links(item)
        if (links.isNotEmpty()) {
            LinkedText(
                modifier = Modifier.padding(cellPadding),
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
    fun links(item: T): List<Link>

    /**
     * Invoked when the user clicks one of the rendered links.
     */
    fun onClickLink(link: String)
}
