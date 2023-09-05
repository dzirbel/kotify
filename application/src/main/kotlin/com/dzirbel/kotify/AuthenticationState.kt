package com.dzirbel.kotify

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import com.dzirbel.kotify.network.oauth.AccessToken
import com.dzirbel.kotify.repository.player.PlayerRepository
import com.dzirbel.kotify.repository.savedRepositories
import com.dzirbel.kotify.repository.user.UserRepository

enum class AuthenticationState {
    UNAUTHENTICATED,
    LOADING_USER,
    AUTHENTICATED,
}

@Composable
fun WithAuthentication(content: @Composable (AuthenticationState) -> Unit) {
    val tokenState = AccessToken.Cache.tokenFlow.collectAsState()
    val userIdState = UserRepository.currentUserId.collectAsState()

    val authenticationState = remember {
        derivedStateOf {
            when {
                tokenState.value == null -> AuthenticationState.UNAUTHENTICATED
                userIdState.value == null -> AuthenticationState.LOADING_USER
                else -> AuthenticationState.AUTHENTICATED
            }
        }
    }
        .value

    if (authenticationState != AuthenticationState.UNAUTHENTICATED) {
        LaunchedEffect(Unit) { UserRepository.ensureCurrentUserLoaded() }
    }

    if (authenticationState == AuthenticationState.AUTHENTICATED) {
        LaunchedEffect(Unit) { onSignedIn() }
    }

    content(authenticationState)
}

/**
 * Holds initialization logic which should only be run when there is a signed-in user. Should only be called once per
 * user session, in particular:
 * - on or near after application start if there is already is signed-in user
 * - NOT on application start if there is no signed-in user
 * - on user sign in after application start without a signed-in user
 * - again on any subsequent sign ins (after sign-outs)
 */
private fun onSignedIn() {
    // load initial player state
    PlayerRepository.refreshPlayback()
    PlayerRepository.refreshTrack()
    PlayerRepository.refreshDevices()

    savedRepositories.forEach { it.init() }
}
