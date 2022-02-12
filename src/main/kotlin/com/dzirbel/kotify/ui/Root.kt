package com.dzirbel.kotify.ui

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
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
import com.dzirbel.kotify.ui.page.artists.ArtistsPage
import com.dzirbel.kotify.ui.panel.debug.DebugPanel
import com.dzirbel.kotify.ui.panel.library.LibraryPanel
import com.dzirbel.kotify.ui.panel.navigation.NavigationPanel
import com.dzirbel.kotify.ui.player.PlayerPanel
import com.dzirbel.kotify.ui.unauthenticated.Unauthenticated

/**
 * State of the [PageStack], available globally for convenience, rather than passing it into each function which
 * navigates through the PageStack. Might ideally be encapsulated in something like a CompositionLocalProvider but that
 * would prevent mutation outside of a @Composable context.
 */
val pageStack: MutableState<PageStack> = mutableStateOf(PageStack(ArtistsPage))

private val libraryPanelSize = PanelSize(
    initialSize = FixedOrPercent.Fixed(300.dp),
    minPanelSizeDp = 150.dp,
    minContentSizePercent = 0.7f
)

@Composable
fun Root() {
    if (AccessToken.Cache.hasToken) {
        DebugPanel {
            Column {
                SidePanel(
                    modifier = Modifier.fillMaxHeight().weight(1f),
                    direction = PanelDirection.LEFT,
                    panelSize = libraryPanelSize,
                    panelContent = { LibraryPanel() },
                    mainContent = {
                        Column {
                            val page = pageStack.value.current
                            val headerVisibleState = remember(page) { MutableTransitionState(false) }

                            NavigationPanel(
                                headerVisibleState = headerVisibleState,
                                headerContent = {
                                    with(page) {
                                        headerContent()
                                    }
                                },
                            )

                            Box(Modifier.fillMaxSize().weight(1f)) {
                                with(page) {
                                    content(toggleHeader = { headerVisibleState.targetState = it })
                                }
                            }
                        }
                    }
                )

                PlayerPanel()
            }
        }
    } else {
        Unauthenticated()
    }
}
