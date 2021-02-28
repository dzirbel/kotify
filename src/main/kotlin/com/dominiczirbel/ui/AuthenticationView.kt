package com.dominiczirbel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dominiczirbel.network.oauth.OAuth
import kotlinx.coroutines.launch
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor

data class AuthenticationViewModel(
    val clientId: String = OAuth.DEFAULT_CLIENT_ID,
    val oauthState: OAuth? = null,
    val redirectUri: String = ""
)

private val PADDING = 10.dp
private const val DIALOG_WIDTH = 400
private const val DIALOG_HEIGHT = 400

@Composable
fun AuthenticationDialog(
    onDismissRequest: () -> Unit,
    onAuthenticated: () -> Unit
) {
    Dialog(
        properties = DialogProperties(
            title = "Spotify API Authentication",
            size = IntSize(DIALOG_WIDTH, DIALOG_HEIGHT)
        ),
        onDismissRequest = onDismissRequest
    ) {
        AuthenticationView(onAuthenticated = onAuthenticated)
    }
}

@Composable
fun AuthenticationView(
    onAuthenticated: () -> Unit
) {
    val viewModel = remember { mutableStateOf(AuthenticationViewModel()) }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(PADDING), verticalArrangement = Arrangement.spacedBy(PADDING, Alignment.Top)) {
        val oauthState = viewModel.value.oauthState
        if (oauthState != null) {
            Text("Flow in progress")

            Text("Authorize the Spotify API and then paste the resulting redirected url here:")

            // TODO add button to cancel and restart
            // TODO add redirect url in case the default open did not work

            Button(
                onClick = {
                    val clipboard = Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor)
                    viewModel.value = viewModel.value.copy(redirectUri = clipboard as String)
                }
            ) {
                Text("Paste")
            }

            TextField(
                value = viewModel.value.redirectUri,
                modifier = Modifier.fillMaxWidth(),
                onValueChange = { viewModel.value = viewModel.value.copy(redirectUri = it) },
                label = {
                    Text("Redirect URI")
                }
            )

            // TODO loading state
            Button(
                enabled = viewModel.value.redirectUri.isNotEmpty(),
                onClick = {
                    coroutineScope.launch {
                        // TODO handle failure (and errors)
                        oauthState.onRedirect(redirectedUri = viewModel.value.redirectUri)
                        onAuthenticated()
                    }
                }
            ) {
                Text("Finish")
            }
        } else {
            // TODO add ID/secret option (and others?)

            TextField(
                value = viewModel.value.clientId,
                modifier = Modifier.fillMaxWidth(),
                onValueChange = { viewModel.value = viewModel.value.copy(clientId = it) },
                label = {
                    Text("Client ID")
                }
            )

            Button(
                onClick = {
                    viewModel.value = viewModel.value.copy(oauthState = OAuth.start())
                }
            ) {
                Text("Start OAuth Flow")
            }
        }
    }
}
