package com.dominiczirbel.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Suppress("MagicNumber")
@Composable
fun Root() {
    val leftPanelState = remember { PanelState(initialSize = 500.dp, minSize = 200.dp, minContentSize = 500.dp) }
    val pageStack = remember { mutableStateOf(PageStack(ArtistsPage)) }

    Column {
        SidePanel(
            modifier = Modifier.fillMaxHeight().weight(1f),
            direction = PanelDirection.LEFT,
            state = leftPanelState,
            panelContent = { LibraryPanel(pageStack = pageStack) },
            mainContent = { MainContent(pageStack = pageStack) }
        )

        BottomPanel()
    }
}
