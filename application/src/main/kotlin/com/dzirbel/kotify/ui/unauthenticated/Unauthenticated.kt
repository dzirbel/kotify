package com.dzirbel.kotify.ui.unauthenticated

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.Application
import com.dzirbel.kotify.Settings
import com.dzirbel.kotify.network.oauth.OAuth
import com.dzirbel.kotify.ui.components.ProjectGithubIcon
import com.dzirbel.kotify.ui.components.ThemeSwitcher
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.theme.surfaceBackground
import com.dzirbel.kotify.ui.util.openInBrowser
import kotlinx.coroutines.launch

private val MIN_WIDTH = 500.dp
private const val WIDTH_FRACTION = 0.5f

/**
 * Displays the un-authenticated landing page and authentication flow.
 */
@Composable
fun Unauthenticated() {
    LocalColors.current.WithSurface {
        Box(Modifier.fillMaxSize().surfaceBackground()) {
            val scrollState = rememberScrollState()
            Box(Modifier.verticalScroll(scrollState).fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .padding(vertical = Dimens.space5)
                        .defaultMinSize(minWidth = MIN_WIDTH)
                        .fillMaxWidth(fraction = WIDTH_FRACTION)
                        .fillMaxHeight()
                        .align(Alignment.TopCenter),
                    verticalArrangement = Arrangement.spacedBy(Dimens.space4),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.space3, Alignment.End),
                    ) {
                        ThemeSwitcher(onSetColors = { Settings.colors = it })
                        ProjectGithubIcon(githubUrl = Application.github)
                    }

                    UnauthenticatedContent()
                }
            }

            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd),
                adapter = rememberScrollbarAdapter(scrollState),
            )
        }
    }
}

@Composable
private fun UnauthenticatedContent() {
    var authenticationParams by remember { mutableStateOf(AuthenticationParams()) }
    val oauthState = remember { mutableStateOf<OAuth?>(null) }
    val oauth = oauthState.value
    if (oauth == null) {
        LandingPage(
            params = authenticationParams,
            onSetParams = { authenticationParams = it },
            onStartOAuth = {
                oauthState.value = OAuth.start(
                    clientId = authenticationParams.clientId,
                    port = authenticationParams.port,
                    runLocalServer = authenticationParams.runLocalServer,
                    scopes = authenticationParams.scopes,
                    openAuthorizationUrl = { openInBrowser(it.toUri()) },
                )
            },
        )
    } else {
        var manualRedirectLoading by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        FlowInProgress(
            oauthError = oauth.errorFlow.collectAsState().value,
            oauthResult = oauth.resultFlow.collectAsState().value,
            authorizationUrl = oauth.authorizationUrl.toString(),
            manualRedirectLoading = manualRedirectLoading,
            onCancel = {
                oauth.cancel()
                oauthState.value = null
            },
            onManualRedirect = { uri ->
                manualRedirectLoading = true
                scope.launch {
                    oauth.onManualRedirect(uri)
                    manualRedirectLoading = false
                }
            },
        )
    }
}
