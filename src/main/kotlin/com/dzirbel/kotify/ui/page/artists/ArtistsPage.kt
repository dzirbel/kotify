package com.dzirbel.kotify.ui.page.artists

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.dzirbel.kotify.ui.components.Page
import com.dzirbel.kotify.ui.components.PageStack

object ArtistsPage : Page {
    override fun toString() = "Saved Artists"

    @Composable
    override fun BoxScope.content(pageStack: MutableState<PageStack>, toggleHeader: (Boolean) -> Unit) {
        Artists(pageStack, toggleHeader)
    }

    @Composable
    override fun RowScope.headerContent(pageStack: MutableState<PageStack>) {
        Text("Artists")
    }
}
