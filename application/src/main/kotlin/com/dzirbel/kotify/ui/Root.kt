package com.dzirbel.kotify.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.AuthenticationState
import com.dzirbel.kotify.Settings
import com.dzirbel.kotify.ui.components.panel.FixedOrPercent
import com.dzirbel.kotify.ui.components.panel.PanelDirection
import com.dzirbel.kotify.ui.components.panel.PanelSize
import com.dzirbel.kotify.ui.components.panel.SidePanel
import com.dzirbel.kotify.ui.page.PageStack
import com.dzirbel.kotify.ui.page.PageStackContent
import com.dzirbel.kotify.ui.page.artists.ArtistsPage
import com.dzirbel.kotify.ui.panel.debug.DebugPanel
import com.dzirbel.kotify.ui.panel.library.LibraryPanel
import com.dzirbel.kotify.ui.panel.navigation.NavigationPanel
import com.dzirbel.kotify.ui.player.PlayerPanel
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.KotifyTheme
import com.dzirbel.kotify.ui.unauthenticated.Unauthenticated
import com.dzirbel.kotify.util.immutable.mapToImmutableList

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
fun Root(authenticationState: AuthenticationState) {
    InvalidatingRootContent {
        KotifyTheme.Apply(colors = Settings.colors) {
            when (authenticationState) {
                AuthenticationState.UNAUTHENTICATED -> Unauthenticated()

                AuthenticationState.LOADING_USER ->
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimens.space3, Alignment.CenterVertically),
                    ) {
                        // TODO expose errors when loading current user ID
                        CircularProgressIndicator(Modifier.size(Dimens.iconLarge))

                        val userId = LocalUserRepository.current.currentUserId.collectAsState().value
                        Text("Loading user data for $userId")
                    }

                AuthenticationState.AUTHENTICATED ->
                    DebugPanel {
                        Column {
                            SidePanel(
                                modifier = Modifier.fillMaxHeight().weight(1f),
                                direction = PanelDirection.LEFT,
                                panelSize = libraryPanelSize,
                                panelContent = { LibraryPanel() },
                            ) {
                                PageStackAndNavigationPanel()
                            }

                            PlayerPanel()
                        }
                    }
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
private fun PageStackAndNavigationPanel() {
    val pageStack = pageStack.value

    // hacky, but create (and remember) individual states for the title and header visibility of each page
    val titles = List(pageStack.pages.size) { pageIndex ->
        remember(pageIndex) { mutableStateOf<String?>(null) }
    }
    val navigationTitleVisibilityStates = List(pageStack.pages.size) { pageIndex ->
        remember(pageIndex) { mutableStateOf(false) }
    }
    val currentNavigationTitleVisibilityState = navigationTitleVisibilityStates[pageStack.currentIndex]

    Surface {
        Column {
            NavigationPanel(
                titleVisible = currentNavigationTitleVisibilityState.value,
                titles = titles.mapToImmutableList { { it.value } },
            )

            PageStackContent(
                pageStack = pageStack,
                setTitle = { index, title ->
                    titles[index].value = title
                },
                setNavigationTitleVisible = { visible ->
                    currentNavigationTitleVisibilityState.value = visible
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
