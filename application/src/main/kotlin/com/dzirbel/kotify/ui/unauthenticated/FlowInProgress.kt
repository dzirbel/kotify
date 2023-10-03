package com.dzirbel.kotify.ui.unauthenticated

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import com.dzirbel.kotify.network.oauth.LocalOAuthServer
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.components.CopyButton
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.KotifyTypography
import com.dzirbel.kotify.ui.util.consumeKeyEvents
import com.dzirbel.kotify.ui.util.getClipboard
import com.dzirbel.kotify.util.takingIf
import java.net.URI

@Composable
fun FlowInProgress(
    oauthError: Throwable?,
    oauthResult: LocalOAuthServer.Result?,
    authorizationUrl: String,
    manualRedirectLoading: Boolean,
    onManualRedirect: (uri: URI) -> Unit,
    onCancel: () -> Unit,
) {
    Column {
        if (oauthError == null) {
            if (oauthResult != null) {
                val errorMessage = when (oauthResult) {
                    is LocalOAuthServer.Result.Error -> "Error: ${oauthResult.error}"

                    is LocalOAuthServer.Result.MismatchedState ->
                        "Mismatched state: expected `${oauthResult.expectedState}` but was `${oauthResult.actualState}`"

                    is LocalOAuthServer.Result.Success -> null
                }

                if (errorMessage == null) {
                    Text("Authentication complete...")
                } else {
                    Text(errorMessage, color = MaterialTheme.colors.error)
                }
            } else {
                Text("Authentication in progress. Accept the OAuth request from Spotify in your browser to continue.")
            }
        } else {
            Text(
                text = "Error during authentication!",
                color = MaterialTheme.colors.error,
                style = MaterialTheme.typography.h5,
            )

            Text(
                text = oauthError.stackTraceToString(),
                color = MaterialTheme.colors.error,
                fontFamily = KotifyTypography.Monospace,
            )
        }

        Spacer(Modifier.height(Dimens.space4))

        Button(onClick = onCancel, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            CachedIcon("cancel", size = Dimens.iconSmall)
            Spacer(Modifier.width(Dimens.space3))
            Text("Cancel")
        }

        Spacer(Modifier.height(Dimens.space5))

        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space3)) {
            Text("If your browser was not opened automatically, copy this URL:")

            OutlinedTextField(
                value = authorizationUrl,
                onValueChange = {},
                modifier = Modifier.consumeKeyEvents().fillMaxWidth(),
                singleLine = true,
                readOnly = true,
                label = { Text("Authorization URL") },
                trailingIcon = { CopyButton(authorizationUrl) },
            )
        }

        Spacer(Modifier.height(Dimens.space4))

        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space3)) {
            @Suppress("StringShouldBeRawString")
            Text(
                "If you've accepted the authorization request in Spotify but it wasn't automatically captured (the " +
                    "browser may be showing a \"site can't be reached\" error), copy the entire URL here (it should " +
                    "start with \"localhost:\"):",
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.space3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                var manualRedirectUrlString by remember { mutableStateOf("") }
                val manualRedirectUri = takingIf(manualRedirectUrlString.isNotBlank()) {
                    runCatching { URI(manualRedirectUrlString) }.getOrNull()
                }

                OutlinedTextField(
                    value = manualRedirectUrlString,
                    modifier = Modifier.weight(1f).consumeKeyEvents(),
                    singleLine = true,
                    onValueChange = { manualRedirectUrlString = it },
                    label = {
                        Text("Redirect URL")
                    },
                    trailingIcon = {
                        IconButton(
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Default),
                            onClick = { manualRedirectUrlString = getClipboard() },
                        ) {
                            CachedIcon(name = "content-paste", contentDescription = "Paste")
                        }
                    },
                )

                Button(
                    enabled = !manualRedirectLoading && manualRedirectUri != null,
                    onClick = { manualRedirectUri?.let(onManualRedirect) },
                ) {
                    if (manualRedirectLoading) {
                        CircularProgressIndicator(Modifier.size(Dimens.iconSmall))
                    } else {
                        CachedIcon("check-circle", size = Dimens.iconSmall)
                    }

                    Spacer(Modifier.width(Dimens.space2))

                    Text("Submit")
                }
            }
        }
    }
}
