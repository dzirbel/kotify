package com.dzirbel.kotify.ui.page.tracks

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.dzirbel.kotify.ui.components.Page
import com.dzirbel.kotify.ui.components.PageStack

object TracksPage : Page {
    override fun toString() = "Saved Tracks"

    @Composable
    override fun BoxScope.content(pageStack: MutableState<PageStack>, toggleHeader: (Boolean) -> Unit) {
        Tracks(pageStack)
    }
}
