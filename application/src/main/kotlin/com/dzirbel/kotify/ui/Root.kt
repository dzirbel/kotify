package com.dzirbel.kotify.ui

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.network.oauth.AccessToken
import com.dzirbel.kotify.repository.player.PlayerRepository
import com.dzirbel.kotify.repository.savedRepositories
import com.dzirbel.kotify.repository.user.UserRepository
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
import com.dzirbel.kotify.ui.theme.surfaceBackground
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

/**
 * Holds initialization logic which should only be run when there is a signed-in user. Should only be called once per
 * user session, in particular:
 * - on or near after application start if there is already is signed-in user
 * - NOT on application start if there is no signed-in user
 * - on user sign in after application start without a signed-in user
 * - again on any subsequent sign ins (after sign-outs)
 */
fun onSignedIn() {
    check(UserRepository.hasCurrentUserId)

    // load initial player state
    PlayerRepository.refreshPlayback()
    PlayerRepository.refreshTrack()
    PlayerRepository.refreshDevices()

    savedRepositories.forEach { it.init() }
}

@Composable
fun Root() {
    InvalidatingRootContent {
        Theme.Apply {
            val tokenState = AccessToken.Cache.tokenFlow.collectAsState()
            val hasToken = tokenState.value != null
            val hasCurrentUserId = UserRepository.currentUserId.collectAsState().value != null

            if (hasToken) {
                LaunchedEffect(tokenState.value?.accessToken) {
                    UserRepository.ensureCurrentUserLoaded()
                }
            }

            if (hasCurrentUserId) {
                LaunchedEffect(Unit) {
                    onSignedIn()
                }
            }

            // only show authenticated content when the current user ID has been fetched to avoid race conditions
            // fetching it
            if (hasToken && hasCurrentUserId) {
                DebugPanel {
                    Column {
                        SidePanel(
                            modifier = Modifier.fillMaxHeight().weight(1f),
                            direction = PanelDirection.LEFT,
                            panelSize = libraryPanelSize,
                            panelContent = { LibraryPanel() },
                            mainContent = { PageStackAndNavigationPanel() },
                        )

                        PlayerPanel()
                    }
                }
            } else if (hasToken) {
                // TODO add loading screen / overlay if token is available but user ID is not
                // TODO expose errors when loading current user ID
                Text("Loading current user...")
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
private fun PageStackAndNavigationPanel() {
    val pageStack = pageStack.value

    // hacky, but create (and remember) individual states for the title and header visibility of each page
    val titles = List(pageStack.pages.size) { pageIndex ->
        remember(pageIndex) { mutableStateOf<String?>(null) }
    }
    val navigationTitleVisibilityStates = List(pageStack.pages.size) { pageIndex ->
        remember(pageIndex) { MutableTransitionState(false) }
    }
    val currentNavigationTitleVisibilityState = navigationTitleVisibilityStates[pageStack.currentIndex]

    Column(modifier = Modifier.surfaceBackground()) {
        NavigationPanel(
            titleVisibilityState = currentNavigationTitleVisibilityState,
            titles = titles.mapToImmutableList { { it.value } },
        )

        PageStackContent(
            pageStack = pageStack,
            setTitle = { index, title ->
                titles[index].value = title
            },
            setNavigationTitleVisible = { visible ->
                currentNavigationTitleVisibilityState.targetState = visible
            },
            modifier = Modifier.weight(1f),
        )
    }
}
