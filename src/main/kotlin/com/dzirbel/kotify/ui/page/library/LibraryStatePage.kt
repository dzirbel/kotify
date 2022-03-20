package com.dzirbel.kotify.ui.page.library

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import com.dzirbel.kotify.ui.components.Page

object LibraryStatePage : Page<Unit> {
    @Composable
    override fun BoxScope.bind(visible: Boolean, toggleNavigationTitle: (Boolean) -> Unit) {
        // TODO doesn't bind presenters
        val scrollState = rememberScrollState()

        if (visible) {
            LibraryState(scrollState)
        }
    }

    override fun titleFor(data: Unit) = "Library State"
}
