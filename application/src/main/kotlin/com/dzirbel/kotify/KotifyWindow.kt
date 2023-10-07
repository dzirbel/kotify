package com.dzirbel.kotify

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import com.dzirbel.kotify.ui.IconCache
import com.dzirbel.kotify.ui.KeyboardShortcuts
import com.dzirbel.kotify.ui.LocalPlayer
import com.dzirbel.kotify.ui.ProvideRepositories
import com.dzirbel.kotify.ui.Root

/**
 * Sets up the main [Window], in particular toggling it between floating and maximized based on authentication state.
 */
@Composable
fun ApplicationScope.KotifyWindow() {
    ProvideRepositories {
        WithAuthentication { authenticationState ->
            // TODO on linux, un-maximizing from the maximized state does not return to the original size when the
            //  window was first initialized as maximized
            //  see https://github.com/JetBrains/compose-multiplatform/issues/3620
            val windowState = if (authenticationState == AuthenticationState.AUTHENTICATED) {
                WindowState(
                    placement = WindowPlacement.Maximized,

                    // set to unspecified to avoid initial composition with unauthenticated size
                    size = DpSize.Unspecified,

                    // should be a no-op, but appears required to have the floating window align on un-maximize
                    position = WindowPosition.Aligned(Alignment.Center),
                )
            } else {
                WindowState(
                    placement = WindowPlacement.Floating,
                    size = unauthenticatedWindowSize,
                    position = WindowPosition.Aligned(Alignment.Center),
                )
            }

            val keyboardShortcuts = KeyboardShortcuts(LocalPlayer.current)

            Window(
                onCloseRequest = ::exitApplication,
                title = "${Application.name} ${Application.version}",
                state = windowState,
                icon = IconCache.logo,
                onKeyEvent = keyboardShortcuts::handle,
                content = { Root(authenticationState) },
            )
        }
    }
}

private val unauthenticatedWindowSize = DpSize(width = 600.dp, height = 800.dp)
