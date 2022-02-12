package com.dzirbel.kotify.ui.page.tracks

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import com.dzirbel.kotify.ui.components.Page

object TracksPage : Page {
    override fun toString() = "Saved Tracks"

    @Composable
    override fun BoxScope.content(toggleHeader: (Boolean) -> Unit) = Tracks()
}
