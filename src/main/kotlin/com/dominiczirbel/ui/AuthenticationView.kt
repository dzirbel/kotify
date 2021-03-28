package com.dominiczirbel.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dominiczirbel.network.oauth.LocalOAuthServer
import com.dominiczirbel.network.oauth.OAuth
import com.dominiczirbel.ui.common.CheckboxWithLabel
import com.dominiczirbel.ui.theme.Colors
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.ui.util.mutate
import kotlinx.coroutines.launch

data class AuthenticationState(
    val oauth: OAuth? = null,
    val clientId: String = OAuth.DEFAULT_CLIENT_ID,
    val port: Int = LocalOAuthServer.DEFAULT_PORT,
    val scopes: Set<String> = OAuth.DEFAULT_SCOPES.toSet(),
    val manualRedirectUrl: String = ""
)

private val MIN_WIDTH = 500.dp
private const val WIDTH_FRACTION = 0.5f

// TODO automatically reset authentication view state when OAuth is consumed (e.g. on error)

@Composable
fun AuthenticationView() {
    val state = remember { mutableStateOf(AuthenticationState()) }
    val scrollState: ScrollState = rememberScrollState(0)

    Box(Modifier.fillMaxSize().background(Colors.current.surface2)) {
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(vertical = Dimens.space5)
                .defaultMinSize(minWidth = MIN_WIDTH)
                .fillMaxWidth(fraction = WIDTH_FRACTION)
                .fillMaxHeight()
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(Dimens.space3, Alignment.Top)
        ) {
            if (state.value.oauth == null) {
                Welcome(state)
            } else {
                FlowInProgress(state)
            }
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd),
            adapter = rememberScrollbarAdapter(scrollState)
        )
    }
}

@Composable
fun Welcome(state: MutableState<AuthenticationState>) {
    Text("Welcome!", fontSize = Dimens.fontTitle)

    Text(
        "To get started, you'll need to authenticate with Spotify. " +
            "This will open a web browser to request the permissions this application needs to function."
    )

    Button(
        onClick = {
            state.mutate { copy(oauth = OAuth.start(clientId = clientId, port = port, scopes = scopes.toList())) }
        }
    ) {
        Text("Authenticate")
    }

    Spacer(Modifier.height(Dimens.space5))

    // TODO collapsible
    Text("Technical details:")

    TextField(
        value = state.value.clientId,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        onValueChange = { state.mutate { copy(clientId = it) } },
        label = {
            Text("Client ID")
        }
    )

    TextField(
        value = state.value.port.toString(),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        onValueChange = { state.mutate { copy(port = it.toInt()) } },
        label = {
            Text("Localhost port")
        }
    )

    Text("Scopes:")

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

@Composable
fun FlowInProgress(state: MutableState<AuthenticationState>) {
    val oauth = state.value.oauth!!

    Text("Authentication in progress. Accept the OAuth request from Spotify to continue.")

    Text("If it was not opened automatically, copy this URL into your browser:")

    // TODO button to copy to clipboard
    TextField(
        value = oauth.authorizationUrl.toString(),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        readOnly = true,
        onValueChange = { },
        label = {
            Text("Authorization URL")
        }
    )

    Button(
        onClick = {
            runCatching { oauth.cancel() }
            state.mutate { copy(oauth = null) }
        }
    ) {
        Text("Cancel flow")
    }

    Text(
        "If you've accepted the authorization request in Spotify but it wasn't automatically captured, " +
            "copy the localhost url here:"
    )

    TextField(
        value = state.value.manualRedirectUrl,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        onValueChange = { state.mutate { copy(manualRedirectUrl = it) } },
        label = {
            Text("Redirect URL")
        }
    )

    // TODO loading state
    // TODO show result of attempt
    val submitting = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Button(
        enabled = state.value.manualRedirectUrl.isNotEmpty() && !submitting.value,
        onClick = {
            submitting.value = true
            scope.launch {
                oauth.onManualRedirect(url = state.value.manualRedirectUrl)
            }
        }
    ) {
        Text("Submit")
    }
}
