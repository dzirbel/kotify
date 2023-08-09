package com.dzirbel.kotify.ui.unauthenticated

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.Application
import com.dzirbel.kotify.network.oauth.LocalOAuthServer
import com.dzirbel.kotify.network.oauth.OAuth
import com.dzirbel.kotify.ui.components.CheckboxWithLabel
import com.dzirbel.kotify.ui.components.HorizontalSpacer
import com.dzirbel.kotify.ui.components.HyperlinkSpanStyle
import com.dzirbel.kotify.ui.components.LinkedText
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.consumeKeyEvents
import com.dzirbel.kotify.ui.util.mutate
import com.dzirbel.kotify.util.collections.plusOrMinus
import kotlinx.collections.immutable.toPersistentSet

@Composable
fun ColumnScope.LandingPage(
    params: AuthenticationParams,
    onSetParams: (AuthenticationParams) -> Unit,
    onStartOAuth: () -> Unit,
) {
    Text("Welcome to ${Application.name}!", style = MaterialTheme.typography.h5)

    VerticalSpacer(Dimens.space3)

    Text(
        "To get started, you'll need to authenticate with Spotify. This will open a web browser to request the " +
            "permissions ${Application.name} needs to function.",
    )

    Button(
        modifier = Modifier.align(Alignment.CenterHorizontally).padding(Dimens.space3),
        onClick = onStartOAuth,
    ) {
        Text("Authenticate")
    }

    VerticalSpacer(Dimens.space5)

    val detailsExpanded = remember { mutableStateOf(false) }
    SimpleTextButton(
        onClick = {
            detailsExpanded.mutate { !this }
        },
    ) {
        Icon(
            imageVector = if (detailsExpanded.value) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
        )

        HorizontalSpacer(Dimens.space2)

        Text("Technical details")
    }

    if (detailsExpanded.value) {
        TextField(
            modifier = Modifier.consumeKeyEvents(),
            value = params.clientId,
            singleLine = true,
            onValueChange = { onSetParams(params.copy(clientId = it)) },
            label = {
                Text("Client ID")
            },
            trailingIcon = {
                IconButton(
                    enabled = params.clientId != OAuth.DEFAULT_CLIENT_ID,
                    onClick = { onSetParams(params.copy(clientId = OAuth.DEFAULT_CLIENT_ID)) },
                ) {
                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                }
            },
        )

        LinkedText(unhoveredSpanStyle = HyperlinkSpanStyle(), hoveredSpanStyle = HyperlinkSpanStyle()) {
            text(
                "The Spotify application client ID to authenticate with, through which API requests are made. See the ",
            )
            link(text = "docs", link = "https://developer.spotify.com/documentation/general/guides/app-settings/")
            text(" for details.")
        }

        VerticalSpacer(Dimens.space3)

        TextField(
            modifier = Modifier.consumeKeyEvents(),
            value = params.port.toString(),
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
                IconButton(
                    enabled = params.port != LocalOAuthServer.DEFAULT_PORT,
                    onClick = { onSetParams(params.copy(port = LocalOAuthServer.DEFAULT_PORT)) },
                ) {
                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                }
            },
        )

        Text(
            "The port on which a local server is run to capture the OAuth redirect URL. Note that " +
                "`http://localhost:<PORT>/` MUST be whitelisted as a redirect URI for the Spotify client above, " +
                "otherwise the authentication request will be rejected.",
        )

        VerticalSpacer(Dimens.space3)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Scopes", style = MaterialTheme.typography.h5)

            IconButton(
                enabled = params.scopes != OAuth.DEFAULT_SCOPES,
                onClick = { onSetParams(params.copy(scopes = OAuth.DEFAULT_SCOPES.toPersistentSet())) },
            ) {
                Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
            }
        }

        LinkedText(unhoveredSpanStyle = HyperlinkSpanStyle(), hoveredSpanStyle = HyperlinkSpanStyle()) {
            text(
                "The authentication scopes that this application requests; if some are not granted parts of the " +
                    "application may not work. See the ",
            )
            link(text = "docs", link = "https://developer.spotify.com/documentation/general/guides/scopes/")
            text(" for details.")
        }

        for (scope in OAuth.ALL_SCOPES) {
            val checked = scope in params.scopes
            CheckboxWithLabel(
                checked = checked,
                onCheckedChange = { onSetParams(params.copy(scopes = params.scopes.plusOrMinus(scope, !checked))) },
                label = {
                    Text(scope)
                },
            )
        }
    }
}
