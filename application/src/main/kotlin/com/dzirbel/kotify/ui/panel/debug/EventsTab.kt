package com.dzirbel.kotify.ui.panel.debug

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import com.dzirbel.kotify.Logger

@Composable
fun EventsTab(scrollState: ScrollState) {
    EventList(log = Logger.Events, key = Unit, scrollState = scrollState)
}
