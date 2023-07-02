package com.dzirbel.kotify.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.dzirbel.kotify.util.immutable.mapIndexedToImmutableList
import kotlinx.collections.immutable.ImmutableList

/**
 * State of the [PageStack], available globally for convenience, rather than passing it into each function which
 * navigates through the PageStack. Might ideally be encapsulated in something like a CompositionLocalProvider but that
 * would prevent mutation outside of a @Composable context.
 */
val pageStack: MutableState<PageStack> = mutableStateOf(PageStack(ArtistsPage))

private val invalidationCounter = mutableStateOf(0)

fun invalidateRootComposable() {
    invalidationCounter.value++
}

private val libraryPanelSize = PanelSize(
    initialSize = FixedOrPercent.Fixed(300.dp),
    minPanelSizeDp = 150.dp,
    minContentSizePercent = 0.7f,
)

@Composable
fun Root() {
    InvalidatingRootContent {
        Theme.Apply {
            val tokenState = AccessToken.Cache.tokenFlow.collectAsState()
            val hasToken = remember { derivedStateOf { tokenState.value != null } }.value
            if (hasToken) {
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
    }
}

/**
 * Toggles [content] in and out of the composition when [invalidateRootComposable] is called (i.e .[invalidationCounter]
 * is changed), triggering a full recomposition.
 */
@Composable
private fun InvalidatingRootContent(content: @Composable () -> Unit) {
    var currentInvalidation by remember { mutableStateOf(0) }
    if (currentInvalidation != invalidationCounter.value) {
        currentInvalidation = invalidationCounter.value
        // remove content from the composition then immediately recompose with the counters matching each other
    } else {
        content()
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
            /**
             * Extension function to allow proper parameterization of [Page] on [T]; retrieves the page state from
             * [Page.bind] and passes it into [Page.titleFor], returning the resulting value.
             */
            @Composable
            fun <T> Page<T>.bindAndGetTitle(scope: BoxScope, visible: Boolean): String? {
                val data: T = with(scope) {
                    bind(visible = visible)
                }

                return titleFor(data)
            }

            val pageStack = pageStack.value
            lateinit var titles: ImmutableList<String?>
            Box {
                titles = pageStack.pages.mapIndexedToImmutableList { index, page ->
                    page.bindAndGetTitle(scope = this, visible = index == pageStack.currentIndex)
                }
            }

            NavigationPanel(headerVisibleState = pageStack.current.navigationTitleState, titles = titles)
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
