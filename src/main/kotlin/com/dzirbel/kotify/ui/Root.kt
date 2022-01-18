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
import com.dzirbel.kotify.ui.components.panel.FixedOrPercent
import com.dzirbel.kotify.ui.components.panel.PanelDirection
import com.dzirbel.kotify.ui.components.panel.PanelSize
import com.dzirbel.kotify.ui.components.panel.SidePanel

private val leftPanelSize = PanelSize(
    initialSize = FixedOrPercent.Fixed(300.dp),
    minPanelSizeDp = 150.dp,
    minContentSizePercent = 0.7f
)

@Suppress("MagicNumber")
@Composable
fun Root() {
    if (AccessToken.Cache.hasToken) {
        val pageStack = remember { mutableStateOf(PageStack(ArtistsPage)) }

        DebugPanelOrWindow {
            Column {
                SidePanel(
                    modifier = Modifier.fillMaxHeight().weight(1f),
                    direction = PanelDirection.LEFT,
                    panelSize = leftPanelSize,
                    panelContent = { LibraryPanel(pageStack = pageStack) },
                    mainContent = { MainContent(pageStack = pageStack) }
                )

                BottomPanel(pageStack = pageStack)
            }
        }
    } else {
        AuthenticationView()
    }
}
