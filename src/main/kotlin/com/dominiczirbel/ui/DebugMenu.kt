package com.dominiczirbel.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dominiczirbel.network.model.PrivateUser
import com.dominiczirbel.network.oauth.AccessToken
import com.dominiczirbel.ui.theme.Dimens
import java.time.Instant

private val MAX_WIDTH = 1_000.dp

@Composable
fun DebugMenu(user: PrivateUser) {
    val token = AccessToken.Cache.state().value

    // TODO move this logic elsewhere
    val scopes = remember(token) { token?.scope?.split(' ')?.toList() }
    val receivedInstant = remember(token) { token?.received?.let { Instant.ofEpochMilli(it) } }
    val expiresInstant = remember(token) { token?.expiresIn?.let { receivedInstant?.plusSeconds(it) } }

    Column(Modifier.padding(Dimens.space3).widthIn(max = MAX_WIDTH)) {
        Text(
            text = "Authenticated as ${user.displayName} [${user.id}]",
            fontSize = Dimens.fontBody
        )

        Spacer(Modifier.height(Dimens.space2))

        Text(
            text = "Access token: ${token?.accessToken}",
            fontSize = Dimens.fontBody
        )

        Spacer(Modifier.height(Dimens.space2))

        Text(
            text = "Refreshable: ${token?.refreshToken != null}",
            fontSize = Dimens.fontBody
        )

        Spacer(Modifier.height(Dimens.space2))

        Text(
            text = "${scopes?.size} scopes: ${token?.scope}",
            fontSize = Dimens.fontBody
        )

        Spacer(Modifier.height(Dimens.space2))

        Text(
            text = "Received at $receivedInstant; expires at $expiresInstant",
            fontSize = Dimens.fontBody
        )

        Spacer(Modifier.height(Dimens.space3))

        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = { AccessToken.Cache.clear() },
        ) {
            Text(
                text = "Sign out",
                fontSize = Dimens.fontBody
            )
        }
    }
}
