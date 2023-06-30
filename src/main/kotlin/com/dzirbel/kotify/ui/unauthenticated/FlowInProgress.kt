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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import com.dzirbel.kotify.network.oauth.LocalOAuthServer
import com.dzirbel.kotify.network.oauth.OAuth
import com.dzirbel.kotify.ui.components.CopyButton
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.util.consumeKeyEvents
import com.dzirbel.kotify.ui.util.getClipboard
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Composable
fun FlowInProgress(state: AuthenticationState, oauth: OAuth, onSetState: (AuthenticationState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.space3, Alignment.Top)) {
        val error = oauth.errorFlow.collectAsState().value
        if (error == null) {
            Text("Authentication in progress. Accept the OAuth request from Spotify in your browser to continue.")
        } else {
            Text("Error during authentication!", color = LocalColors.current.error, style = MaterialTheme.typography.h5)

            Text(
                text = error.stackTraceToString(),
                color = LocalColors.current.error,
                fontFamily = FontFamily.Monospace,
            )
        }

        VerticalSpacer(Dimens.space3)

        Button(
            onClick = {
                runCatching { oauth.cancel() }
                onSetState(state.copy(oauth = null))
            },
        ) {
            Text("Cancel flow")
        }

        VerticalSpacer(Dimens.space3)

        Text("If your browser was not opened automatically, copy this URL:")

        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space3)) {
            TextField(
                value = oauth.authorizationUrl.toString(),
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
                        contents = oauth.authorizationUrl.toString(),
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
                value = state.manualRedirectUrl,
                modifier = Modifier.weight(1f).consumeKeyEvents(),
                singleLine = true,
                onValueChange = { onSetState(state.copy(manualRedirectUrl = it)) },
                label = {
                    Text("Redirect URL")
                },
            )

            Button(onClick = { onSetState(state.copy(manualRedirectUrl = getClipboard())) }) {
                Text("Paste")
            }
        }

        val submitting = remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        Button(
            enabled = state.manualRedirectUrl.toHttpUrlOrNull() != null && !submitting.value,
            onClick = {
                submitting.value = true
                val url = state.manualRedirectUrl.toHttpUrl()
                scope.launch {
                    oauth.onManualRedirect(url = url)
                    submitting.value = false
                }
            },
        ) {
            if (submitting.value) {
                CircularProgressIndicator()
            } else {
                Text("Submit")
            }
        }

        oauth.resultFlow.collectAsState().value?.let { result ->
            val message = when (result) {
                is LocalOAuthServer.Result.Error -> "Error: ${result.error}"
                is LocalOAuthServer.Result.MismatchedState ->
                    "Mismatched state: expected `${result.expectedState}` but was `${result.actualState}`"
                is LocalOAuthServer.Result.Success -> null
            }

            if (message != null) {
                VerticalSpacer(Dimens.space3)

                Text(message, color = LocalColors.current.error)
            }
        }
    }
}
