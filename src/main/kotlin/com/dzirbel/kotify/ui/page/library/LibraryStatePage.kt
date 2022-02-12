package com.dzirbel.kotify.ui.page.library

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import com.dzirbel.kotify.ui.components.Page

object LibraryStatePage : Page {
    override fun toString() = "Library State"

    @Composable
    override fun BoxScope.content(toggleHeader: (Boolean) -> Unit) = LibraryState()
}
