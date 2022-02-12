package com.dzirbel.kotify.ui.page.artists

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import com.dzirbel.kotify.ui.components.Page

object ArtistsPage : Page {
    override fun toString() = "Saved Artists"

    @Composable
    override fun BoxScope.content(toggleHeader: (Boolean) -> Unit) = Artists(toggleHeader)

    @Composable
    override fun RowScope.headerContent() {
        Text("Artists")
    }
}
