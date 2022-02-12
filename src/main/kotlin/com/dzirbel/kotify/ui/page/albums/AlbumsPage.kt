package com.dzirbel.kotify.ui.page.albums

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import com.dzirbel.kotify.ui.components.Page

object AlbumsPage : Page {
    override fun toString() = "Saved Albums"

    @Composable
    override fun BoxScope.content(toggleHeader: (Boolean) -> Unit) = Albums()
}
