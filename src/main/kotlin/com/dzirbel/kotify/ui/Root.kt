package com.dzirbel.kotify.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.network.oauth.AccessToken
import com.dzirbel.kotify.ui.components.PageStack
import com.dzirbel.kotify.ui.components.PanelDirection
import com.dzirbel.kotify.ui.components.PanelState
import com.dzirbel.kotify.ui.components.SidePanel

@Suppress("MagicNumber")
@Composable
fun Root() {
    if (AccessToken.Cache.hasToken) {
        val leftPanelState = remember { PanelState(initialSize = 300.dp, minSize = 150.dp, minContentSize = 250.dp) }
        val rightPanelState = remember { PanelState(initialSize = 400.dp, minSize = 125.dp, minContentSize = 250.dp) }
        val pageStack = remember { mutableStateOf(PageStack(ArtistsPage)) }

        SidePanel(
            direction = PanelDirection.RIGHT,
            state = rightPanelState,
            panelEnabled = KeyboardShortcuts.debugShown,
            panelContent = { DebugPanel() },
            mainContent = {
                Column {
                    SidePanel(
                        modifier = Modifier.fillMaxHeight().weight(1f),
                        direction = PanelDirection.LEFT,
                        state = leftPanelState,
                        panelContent = { LibraryPanel(pageStack = pageStack) },
                        mainContent = { MainContent(pageStack = pageStack) }
                    )

                    BottomPanel(pageStack = pageStack)
                }
            }
        )
    } else {
        AuthenticationView()
    }
}
