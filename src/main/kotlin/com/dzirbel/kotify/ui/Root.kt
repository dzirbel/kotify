package com.dzirbel.kotify.ui

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.network.oauth.AccessToken
import com.dzirbel.kotify.ui.components.panel.FixedOrPercent
import com.dzirbel.kotify.ui.components.panel.PanelDirection
import com.dzirbel.kotify.ui.components.panel.PanelSize
import com.dzirbel.kotify.ui.components.panel.SidePanel
import com.dzirbel.kotify.ui.framework.Page
import com.dzirbel.kotify.ui.framework.PageStack
import com.dzirbel.kotify.ui.page.artists.ArtistsPage
import com.dzirbel.kotify.ui.panel.debug.DebugPanel
import com.dzirbel.kotify.ui.panel.library.LibraryPanel
import com.dzirbel.kotify.ui.panel.navigation.NavigationPanel
import com.dzirbel.kotify.ui.player.PlayerPanel
import com.dzirbel.kotify.ui.theme.surfaceBackground
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
    minContentSizePercent = 0.7f,
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
                    mainContent = { PageStackContent() },
                )

                PlayerPanel()
            }
        }
    } else {
        Unauthenticated()
    }
}

/**
 * Wraps [NavigationPanel] and content of the [pageStack], which are connected since the [pageStack] provides the page
 * titles rendered in the [NavigationPanel].
 */
@Composable
private fun PageStackContent() {
    // use a custom Layout in order to render the page stack first, but place it below the navigation panel (which
    // depends on the page stack for its titles)
    Layout(
        modifier = Modifier.surfaceBackground(),
        content = {
            val pageStack = pageStack.value

            // whether the header text in the navigation panel is visible; toggled on by the page when the user scrolls
            // beyond the page's header
            val navigationTitleVisibleState = remember(pageStack.current) { MutableTransitionState(false) }

            /**
             * Extension function to allow proper parameterization of [Page] on [T]; retrieves the page state from
             * [Page.bind] and passes it into [Page.titleFor], returning the resulting value.
             */
            @Composable
            fun <T> Page<T>.bindAndGetTitle(scope: BoxScope, visible: Boolean): String? {
                val data: T = with(scope) {
                    bind(
                        visible = visible,
                        toggleNavigationTitle = { navigationTitleVisibleState.targetState = it },
                    )
                }

                return titleFor(data)
            }

            lateinit var titles: List<String?>
            Box {
                titles = pageStack.pages.mapIndexed { index, page ->
                    page.bindAndGetTitle(scope = this, visible = index == pageStack.currentIndex)
                }
            }

            NavigationPanel(headerVisibleState = navigationTitleVisibleState, titles = titles)
        },
    ) { measurables, constraints ->
        assert(measurables.size == 2)

        val headerPlaceable = measurables[1].measure(constraints)
        val contentPlaceable = measurables[0].measure(
            constraints.copy(maxHeight = constraints.maxHeight - headerPlaceable.height),
        )

        layout(width = constraints.maxWidth, height = constraints.maxHeight) {
            headerPlaceable.place(0, 0)
            contentPlaceable.place(x = 0, y = headerPlaceable.height)
        }
    }
}
