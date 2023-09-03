package com.dzirbel.kotify.ui.panel.debug

import androidx.compose.runtime.Composable
import com.dzirbel.kotify.EventLog

@Composable
fun EventsTab() {
    LogList(EventLog)
}
