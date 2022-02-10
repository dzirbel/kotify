package com.dzirbel.kotify.ui.unauthenticated

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import com.dzirbel.kotify.network.oauth.LocalOAuthServer
import com.dzirbel.kotify.network.oauth.OAuth
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.util.getClipboard
import com.dzirbel.kotify.ui.util.mutate
import com.dzirbel.kotify.ui.util.setClipboard
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Composable
fun FlowInProgress(state: MutableState<AuthenticationState>, oauth: OAuth) {
    if (oauth.error.value == null) {
        Text("Authentication in progress. Accept the OAuth request from Spotify in your browser to continue.")
    } else {
        Text("Error during authentication!", color = LocalColors.current.error, fontSize = Dimens.fontTitle)

        Text(
            text = oauth.error.value!!.stackTraceToString(),
            color = LocalColors.current.error,
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

            Text(it, color = LocalColors.current.error)
        }
    }
}
