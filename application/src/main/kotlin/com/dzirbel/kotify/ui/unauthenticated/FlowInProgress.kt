package com.dzirbel.kotify.ui.unauthenticated

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import com.dzirbel.kotify.network.oauth.LocalOAuthServer
import com.dzirbel.kotify.ui.components.CopyButton
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.KotifyTypography
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.util.consumeKeyEvents
import com.dzirbel.kotify.ui.util.getClipboard
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Composable
fun FlowInProgress(
    oauthErrorState: State<Throwable?>,
    oauthResultState: State<LocalOAuthServer.Result?>,
    authorizationUrl: String,
    manualRedirectUrl: String,
    manualRedirectLoading: Boolean,
    setManualRedirectUrl: (String) -> Unit,
    onCancel: () -> Unit,
    onManualRedirect: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.space3, Alignment.Top)) {
        val oauthError = oauthErrorState.value
        if (oauthError == null) {
            Text("Authentication in progress. Accept the OAuth request from Spotify in your browser to continue.")
        } else {
            Text("Error during authentication!", color = LocalColors.current.error, style = MaterialTheme.typography.h5)

            Text(
                text = oauthError.stackTraceToString(),
                color = LocalColors.current.error,
                fontFamily = KotifyTypography.Monospace,
            )
        }

        VerticalSpacer(Dimens.space3)

        Button(onClick = onCancel) {
            Text("Cancel flow")
        }

        VerticalSpacer(Dimens.space3)

        Text("If your browser was not opened automatically, copy this URL:")

        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space3)) {
            TextField(
                value = authorizationUrl,
                modifier = Modifier.weight(1f).consumeKeyEvents(),
                singleLine = true,
                readOnly = true,
                onValueChange = { },
                label = {
                    Text("Authorization URL")
                },
                trailingIcon = {
                    CopyButton(
                        // override text pointer from TextField
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                        contents = authorizationUrl,
                    )
                },
            )
        }

        VerticalSpacer(Dimens.space3)

        @Suppress("StringShouldBeRawString")
        Text(
            "If you've accepted the authorization request in Spotify but it wasn't automatically captured (the " +
                "browser may be showing a \"site can't be reached\" error), copy the entire URL here (should start " +
                "with \"localhost\"):",
        )

        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space3)) {
            TextField(
                value = manualRedirectUrl,
                modifier = Modifier.weight(1f).consumeKeyEvents(),
                singleLine = true,
                onValueChange = setManualRedirectUrl,
                label = {
                    Text("Redirect URL")
                },
            )

            Button(onClick = { setManualRedirectUrl(getClipboard()) }) {
                Text("Paste")
            }
        }

        Button(
            enabled = !manualRedirectLoading && manualRedirectUrl.toHttpUrlOrNull() != null,
            onClick = onManualRedirect,
        ) {
            if (manualRedirectLoading) {
                CircularProgressIndicator()
            } else {
                Text("Submit")
            }
        }

        val oauthResult = oauthResultState.value
        if (oauthResult != null) {
            val message = when (oauthResult) {
                is LocalOAuthServer.Result.Error -> "Error: ${oauthResult.error}"
                is LocalOAuthServer.Result.MismatchedState ->
                    "Mismatched state: expected `${oauthResult.expectedState}` but was `${oauthResult.actualState}`"

                is LocalOAuthServer.Result.Success -> null
            }

            if (message != null) {
                VerticalSpacer(Dimens.space3)

                Text(message, color = LocalColors.current.error)
            }
        }
    }
}
