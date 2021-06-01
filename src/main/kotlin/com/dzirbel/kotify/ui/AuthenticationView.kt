package com.dzirbel.kotify.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.Application
import com.dzirbel.kotify.network.oauth.LocalOAuthServer
import com.dzirbel.kotify.network.oauth.OAuth
import com.dzirbel.kotify.ui.components.CheckboxWithLabel
import com.dzirbel.kotify.ui.components.HorizontalSpacer
import com.dzirbel.kotify.ui.components.HyperlinkSpanStyle
import com.dzirbel.kotify.ui.components.LinkedText
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.theme.Colors
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.getClipboard
import com.dzirbel.kotify.ui.util.mutate
import com.dzirbel.kotify.ui.util.setClipboard
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class AuthenticationState(
    val oauth: OAuth? = null,
    val clientId: String = OAuth.DEFAULT_CLIENT_ID,
    val port: Int = LocalOAuthServer.DEFAULT_PORT,
    val scopes: Set<String> = OAuth.DEFAULT_SCOPES,
    val manualRedirectUrl: String = ""
)

private val MIN_WIDTH = 500.dp
private const val WIDTH_FRACTION = 0.5f

@Composable
fun AuthenticationView() {
    val state = remember { mutableStateOf(AuthenticationState()) }
    val scrollState: ScrollState = rememberScrollState(0)

    Box(Modifier.fillMaxSize().background(Colors.current.surface2)) {
        Box(Modifier.verticalScroll(scrollState).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(vertical = Dimens.space5)
                    .defaultMinSize(minWidth = MIN_WIDTH)
                    .fillMaxWidth(fraction = WIDTH_FRACTION)
                    .fillMaxHeight()
                    .align(Alignment.TopCenter),
                verticalArrangement = Arrangement.spacedBy(Dimens.space3, Alignment.Top)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    ThemeSwitcher()
                    ProjectGithubIcon()
                }

                val oauth = state.value.oauth
                if (oauth == null) {
                    Welcome(state)
                } else {
                    FlowInProgress(state, oauth)
                }
            }
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd),
            adapter = rememberScrollbarAdapter(scrollState)
        )
    }
}

@Composable
private fun ColumnScope.Welcome(state: MutableState<AuthenticationState>) {
    Text("Welcome to ${Application.name}!", fontSize = Dimens.fontTitle)

    VerticalSpacer(Dimens.space3)

    Text(
        "To get started, you'll need to authenticate with Spotify. This will open a web browser to request the " +
            "permissions ${Application.name} needs to function."
    )

    Button(
        modifier = Modifier.align(Alignment.CenterHorizontally).padding(Dimens.space3),
        onClick = {
            state.mutate { copy(oauth = OAuth.start(clientId = clientId, port = port, scopes = scopes)) }
        }
    ) {
        Text("Authenticate")
    }

    VerticalSpacer(Dimens.space5)

    val detailsExpanded = remember { mutableStateOf(false) }
    SimpleTextButton(
        onClick = {
            detailsExpanded.mutate { !this }
        }
    ) {
        Icon(
            imageVector = if (detailsExpanded.value) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null
        )

        HorizontalSpacer(Dimens.space2)

        Text("Technical details")
    }

    if (detailsExpanded.value) {
        TextField(
            value = state.value.clientId,
            singleLine = true,
            onValueChange = { state.mutate { copy(clientId = it) } },
            label = {
                Text("Client ID")
            },
            trailingIcon = {
                IconButton(
                    enabled = state.value.clientId != OAuth.DEFAULT_CLIENT_ID,
                    onClick = {
                        state.mutate { copy(clientId = OAuth.DEFAULT_CLIENT_ID) }
                    }
                ) {
                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                }
            }
        )

        LinkedText(unhoveredSpanStyle = HyperlinkSpanStyle, hoveredSpanStyle = HyperlinkSpanStyle) {
            text(
                "The Spotify application client ID to authenticate with, through which API requests are made. See the "
            )
            link(text = "docs", link = "https://developer.spotify.com/documentation/general/guides/app-settings/")
            text(" for details.")
        }

        VerticalSpacer(Dimens.space3)

        TextField(
            value = state.value.port.toString(),
            singleLine = true,
            onValueChange = { value ->
                value.toIntOrNull()?.let {
                    state.mutate { copy(port = it) }
                }
            },
            label = {
                Text("Localhost port")
            },
            trailingIcon = {
                IconButton(
                    enabled = state.value.port != LocalOAuthServer.DEFAULT_PORT,
                    onClick = {
                        state.mutate { copy(port = LocalOAuthServer.DEFAULT_PORT) }
                    }
                ) {
                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                }
            }
        )

        Text(
            "The port on which a local server is run to capture the OAuth redirect URL. Note that " +
                "`http://localhost:<PORT>/` MUST be whitelisted as a redirect URI for the Spotify client above, " +
                "otherwise the authentication request will be rejected."
        )

        VerticalSpacer(Dimens.space3)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Scopes", fontSize = Dimens.fontTitle)

            IconButton(
                enabled = state.value.scopes != OAuth.DEFAULT_SCOPES,
                onClick = {
                    state.mutate { copy(scopes = OAuth.DEFAULT_SCOPES) }
                }
            ) {
                Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
            }
        }

        LinkedText(unhoveredSpanStyle = HyperlinkSpanStyle, hoveredSpanStyle = HyperlinkSpanStyle) {
            text(
                "The authentication scopes that this application requests; if some are not granted parts of the " +
                    "application may not work. See the "
            )
            link(text = "docs", link = "https://developer.spotify.com/documentation/general/guides/scopes/")
            text(" for details.")
        }

        for (scope in OAuth.ALL_SCOPES) {
            val checked = scope in state.value.scopes
            CheckboxWithLabel(
                checked = checked,
                onCheckedChange = {
                    state.mutate {
                        copy(scopes = if (checked) scopes.minus(scope) else scopes.plus(scope))
                    }
                },
                label = {
                    Text(scope)
                }
            )
        }
    }
}

@Composable
private fun FlowInProgress(state: MutableState<AuthenticationState>, oauth: OAuth) {
    if (oauth.error.value == null) {
        Text("Authentication in progress. Accept the OAuth request from Spotify in your browser to continue.")
    } else {
        Text("Error during authentication!", color = Colors.current.error, fontSize = Dimens.fontTitle)

        Text(
            text = oauth.error.value!!.stackTraceToString(),
            color = Colors.current.error,
            fontFamily = FontFamily.Monospace
        )
    }

    VerticalSpacer(Dimens.space3)

    Button(
        onClick = {
            runCatching { oauth.cancel() }
            state.mutate { copy(oauth = null) }
        }
    ) {
        Text("Cancel flow")
    }

    VerticalSpacer(Dimens.space3)

    Text("If your browser was not opened automatically, copy this URL:")

    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space3)) {
        TextField(
            value = oauth.authorizationUrl.toString(),
            modifier = Modifier.weight(1f),
            singleLine = true,
            readOnly = true,
            onValueChange = { },
            label = {
                Text("Authorization URL")
            }
        )

        // TODO toast/tooltip/etc when copied
        Button(
            onClick = { setClipboard(oauth.authorizationUrl.toString()) }
        ) {
            Text("Copy to clipboard")
        }
    }

    VerticalSpacer(Dimens.space3)

    Text(
        "If you've accepted the authorization request in Spotify but it wasn't automatically captured (the browser " +
            "may be showing a \"site can't be reached\" error), copy the entire URL here (should start with " +
            "\"localhost\"):"
    )

    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space3)) {
        TextField(
            value = state.value.manualRedirectUrl,
            modifier = Modifier.weight(1f),
            singleLine = true,
            onValueChange = { state.mutate { copy(manualRedirectUrl = it) } },
            label = {
                Text("Redirect URL")
            }
        )

        Button(
            onClick = {
                state.mutate { copy(manualRedirectUrl = getClipboard()) }
            }
        ) {
            Text("Paste")
        }
    }

    val submitting = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Button(
        enabled = state.value.manualRedirectUrl.toHttpUrlOrNull() != null && !submitting.value,
        onClick = {
            submitting.value = true
            val url = state.value.manualRedirectUrl.toHttpUrl()
            scope.launch {
                oauth.onManualRedirect(url = url)
                submitting.value = false
            }
        }
    ) {
        if (submitting.value) {
            CircularProgressIndicator()
        } else {
            Text("Submit")
        }
    }

    oauth.result.value?.let { result ->
        val message = when (result) {
            is LocalOAuthServer.Result.Error -> "Error: ${result.error}"
            is LocalOAuthServer.Result.MismatchedState ->
                "Mismatched state: expected `${result.expectedState}` but was `${result.actualState}`"
            is LocalOAuthServer.Result.Success -> null
        }

        message?.let {
            VerticalSpacer(Dimens.space3)

            Text(it, color = Colors.current.error)
        }
    }
}
