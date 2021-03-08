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
import com.dominiczirbel.ui.theme.Colors
import com.dominiczirbel.ui.theme.Dimens
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import java.time.Instant

private val MAX_WIDTH = 1_000.dp

@FlowPreview
@ExperimentalCoroutinesApi
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
            color = Colors.current.text,
            fontSize = Dimens.fontBody
        )

        Spacer(Modifier.height(Dimens.space2))

        Text(
            text = "Access token: ${token?.accessToken}",
            color = Colors.current.text,
            fontSize = Dimens.fontBody
        )

        Spacer(Modifier.height(Dimens.space2))

        Text(
            text = "Refreshable: ${token?.refreshToken != null}",
            color = Colors.current.text,
            fontSize = Dimens.fontBody
        )

        Spacer(Modifier.height(Dimens.space2))

        Text(
            text = "${scopes?.size} scopes: ${token?.scope}",
            color = Colors.current.text,
            fontSize = Dimens.fontBody
        )

        Spacer(Modifier.height(Dimens.space2))

        Text(
            text = "Received at $receivedInstant; expires at $expiresInstant",
            color = Colors.current.text,
            fontSize = Dimens.fontBody
        )

        Spacer(Modifier.height(Dimens.space3))

        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = { AccessToken.Cache.clear() },
        ) {
            Text(
                text = "Sign out",
                color = Colors.current.text,
                fontSize = Dimens.fontBody
            )
        }
    }
}
