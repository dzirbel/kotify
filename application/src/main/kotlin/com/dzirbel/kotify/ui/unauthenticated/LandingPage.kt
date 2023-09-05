package com.dzirbel.kotify.ui.unauthenticated

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import com.dzirbel.kotify.Application
import com.dzirbel.kotify.network.oauth.LocalOAuthServer
import com.dzirbel.kotify.network.oauth.OAuth
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.components.CheckboxWithLabel
import com.dzirbel.kotify.ui.components.HorizontalDivider
import com.dzirbel.kotify.ui.components.HorizontalSpacer
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.components.TooltipArea
import com.dzirbel.kotify.ui.components.UrlLinkedText
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.consumeKeyEvents
import com.dzirbel.kotify.util.collections.plusOrMinus
import kotlinx.collections.immutable.toPersistentSet

@Composable
fun LandingPage(params: AuthenticationParams, onSetParams: (AuthenticationParams) -> Unit, onStartOAuth: () -> Unit) {
    Column {
        Text("Welcome to ${Application.name}!", style = MaterialTheme.typography.h5)

        VerticalSpacer(Dimens.space4)

        Text(
            "To get started, you'll need to authenticate with Spotify. This will open your web browser to request " +
                "the permissions ${Application.name} needs to function.",
        )

        VerticalSpacer(Dimens.space5)

        Button(modifier = Modifier.align(Alignment.CenterHorizontally), onClick = onStartOAuth) {
            CachedIcon("open-in-new", size = Dimens.iconSmall)
            HorizontalSpacer(Dimens.space3)
            Text("Authenticate")
        }

        VerticalSpacer(Dimens.space5)

        var detailsExpanded by remember { mutableStateOf(false) }
        SimpleTextButton(onClick = { detailsExpanded = !detailsExpanded }) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.space2)) {
                Text("Technical details")

                Icon(
                    imageVector = if (detailsExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                )
            }
        }

        if (detailsExpanded) {
            VerticalSpacer(Dimens.space3)
            Details(params = params, onSetParams = onSetParams)
        }
    }
}

@Composable
fun Details(params: AuthenticationParams, onSetParams: (AuthenticationParams) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.space4)) {
        UrlLinkedText {
            text("Kotify uses ")
            link(text = "OAuth 2.0", link = "https://oauth.net/2/")
            text(
                " to grant permission to read and modify your data in Spotify, without access to your username " +
                    "and password. You may remove Kotify's access at any time on Spotify's ",
            )
            link(text = "\"Manage Apps\"", link = OAuth.SPOTIFY_APPS_URL)
            text(" page.")
        }

        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space3)) {
            OutlinedTextField(
                modifier = Modifier.consumeKeyEvents().fillMaxWidth(),
                value = params.clientId,
                singleLine = true,
                onValueChange = { onSetParams(params.copy(clientId = it)) },
                label = {
                    Text("Client ID")
                },
                trailingIcon = {
                    TooltipArea(tooltip = "Reset to default") {
                        IconButton(
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Default),
                            enabled = params.clientId != OAuth.DEFAULT_CLIENT_ID,
                            onClick = { onSetParams(params.copy(clientId = OAuth.DEFAULT_CLIENT_ID)) },
                        ) {
                            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                        }
                    }
                },
            )

            UrlLinkedText {
                text(
                    "The Spotify application client ID through which API requests are made. By default this uses " +
                        "Kotify's application, but may be changed if this application becomes rate-limited. In this " +
                        "case, you can create your own application in the Spotify application ",
                )
                link(text = "dashboard", link = "https://developer.spotify.com/dashboard")
                text(" and provide its ID here. See Spotify's ")
                link(text = "documentation", link = "https://developer.spotify.com/documentation/web-api/concepts/apps")
                text(" for details.")
            }
        }

        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space3)) {
            OutlinedTextField(
                modifier = Modifier.consumeKeyEvents().fillMaxWidth(),
                value = params.port.toString(),
                enabled = params.runLocalServer,
                singleLine = true,
                onValueChange = { value ->
                    value.toIntOrNull()?.let {
                        onSetParams(params.copy(port = it))
                    }
                },
                label = {
                    Text("Localhost port")
                },
                trailingIcon = {
                    TooltipArea(tooltip = "Reset to default") {
                        IconButton(
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Default),
                            enabled = params.port != LocalOAuthServer.DEFAULT_PORT,
                            onClick = { onSetParams(params.copy(port = LocalOAuthServer.DEFAULT_PORT)) },
                        ) {
                            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                        }
                    }
                },
            )

            Text(
                "During the authentication flow, Kotify runs a local server on this port to capture the URL " +
                    "redirected from Spotify when authentication is granted, which includes the access token. " +
                    "When using a custom client above, the URL `http://localhost:<PORT>/` MUST be listed as a " +
                    "redirect URI in the Spotify application dashboard.",
            )

            Text(
                "If this port is already in use on your machine or you do not wish Kotify to run a local server, it " +
                    "may be disabled below. This will cause the page after authentication is granted to display a " +
                    "404; authentication can proceed by manually copying the redirected URL into Kotify.",
            )

            CheckboxWithLabel(
                checked = params.runLocalServer,
                onCheckedChange = { checked -> onSetParams(params.copy(runLocalServer = checked)) },
            ) {
                Text("Enable localhost redirect URL capture")
            }
        }

        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space3)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Scopes", style = MaterialTheme.typography.h6)

                TooltipArea(tooltip = "Reset to default") {
                    IconButton(
                        enabled = params.scopes != OAuth.Scope.DEFAULT_SCOPES,
                        onClick = { onSetParams(params.copy(scopes = OAuth.Scope.DEFAULT_SCOPES.toPersistentSet())) },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.iconSmall),
                        )
                    }
                }
            }

            UrlLinkedText {
                text(
                    "You may limit the scope of permissions Kotify requests from Spotify, or grant addition ones. " +
                        "This can help protected against malicious use by third-party applications like Kotify, but " +
                        "parts of Kotify may not work without authentication. See Spotify's ",
                )
                link(
                    text = "documentation",
                    link = "https://developer.spotify.com/documentation/web-api/concepts/scopes",
                )
                text(" for details.")
            }

            Text("Required scopes", style = MaterialTheme.typography.body1)

            Text("Scopes which Kotify requests by default and are required for it to function:")

            Column(Modifier.padding(start = Dimens.space2)) {
                for (scope in OAuth.Scope.entries) {
                    if (scope.requestByDefault) {
                        ScopeToggle(scope, params, onSetParams)
                    }
                }
            }

            Text("Other scopes", style = MaterialTheme.typography.body1)

            Text("Scopes which Kotify does not request by default and are not required for it to function:")

            Column(Modifier.padding(start = Dimens.space2)) {
                for (scope in OAuth.Scope.entries) {
                    if (!scope.requestByDefault) {
                        ScopeToggle(scope, params, onSetParams)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScopeToggle(scope: OAuth.Scope, params: AuthenticationParams, onSetParams: (AuthenticationParams) -> Unit) {
    val checked = scope in params.scopes
    CheckboxWithLabel(
        checked = checked,
        onCheckedChange = {
            onSetParams(params.copy(scopes = params.scopes.plusOrMinus(scope, !checked)))
        },
        modifier = Modifier.fillMaxWidth(),
        label = {
            Column {
                Text(scope.scope)
                Text(scope.officialDescription, style = MaterialTheme.typography.caption)
            }
        },
    )
}
