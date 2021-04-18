package com.dzirbel.kotify.ui

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
import com.dzirbel.kotify.network.model.PrivateUser
import com.dzirbel.kotify.network.oauth.AccessToken
import com.dzirbel.kotify.ui.theme.Dimens
import java.time.Instant

private val MAX_WIDTH = 500.dp

@Composable
fun AuthenticationMenu(user: PrivateUser) {
    val token = AccessToken.Cache.token

    // TODO move this logic elsewhere
    val scopes = remember(token) { token?.scope?.split(' ')?.toList() }
    val receivedInstant = remember(token) { token?.received?.let { Instant.ofEpochMilli(it) } }
    val expiresInstant = remember(token) { token?.expiresIn?.let { receivedInstant?.plusSeconds(it) } }

    Column(Modifier.padding(Dimens.space3).widthIn(max = MAX_WIDTH)) {
        Text("Authenticated as ${user.displayName} [${user.id}]")

        Spacer(Modifier.height(Dimens.space2))

        Text("Access token: ${token?.accessToken}")

        Spacer(Modifier.height(Dimens.space2))

        Text("Refreshable: ${token?.refreshToken != null}")

        Spacer(Modifier.height(Dimens.space2))

        Text("${scopes?.size} scopes: ${token?.scope}")

        Spacer(Modifier.height(Dimens.space2))

        Text("Received at $receivedInstant; expires at $expiresInstant")

        Spacer(Modifier.height(Dimens.space3))

        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = { AccessToken.Cache.clear() },
        ) {
            Text("Sign out")
        }
    }
}
