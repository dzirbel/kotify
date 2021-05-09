package com.dzirbel.kotify.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.network.oauth.AccessToken
import com.dzirbel.kotify.ui.components.FixedOrPercent
import com.dzirbel.kotify.ui.components.PageStack
import com.dzirbel.kotify.ui.components.PanelDirection
import com.dzirbel.kotify.ui.components.PanelParams
import com.dzirbel.kotify.ui.components.SidePanel

@Suppress("MagicNumber")
@Composable
fun Root() {
    if (AccessToken.Cache.hasToken) {
        val leftPanelParams = PanelParams(
            initialSize = FixedOrPercent.Fixed(300.dp),
            minPanelSizeDp = 150.dp,
            minContentSizePercent = 0.7f
        )

        val rightPanelParams = PanelParams(
            initialSize = FixedOrPercent.Fixed(400.dp),
            minPanelSizeDp = 125.dp,
            minContentSizePercent = 0.5f
        )

        val pageStack = remember { mutableStateOf(PageStack(ArtistsPage)) }

        SidePanel(
            direction = PanelDirection.RIGHT,
            params = rightPanelParams,
            panelEnabled = KeyboardShortcuts.debugShown,
            panelContent = { DebugPanel() },
            mainContent = {
                Column {
                    SidePanel(
                        modifier = Modifier.fillMaxHeight().weight(1f),
                        direction = PanelDirection.LEFT,
                        params = leftPanelParams,
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
