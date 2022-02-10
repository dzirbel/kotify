package com.dzirbel.kotify.ui.page.albums

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.dzirbel.kotify.ui.components.Page
import com.dzirbel.kotify.ui.components.PageStack

object AlbumsPage : Page {
    override fun toString() = "Saved Albums"

    @Composable
    override fun BoxScope.content(pageStack: MutableState<PageStack>, toggleHeader: (Boolean) -> Unit) {
        Albums(pageStack)
    }
}
