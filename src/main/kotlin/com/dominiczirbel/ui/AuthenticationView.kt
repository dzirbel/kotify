package com.dominiczirbel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dominiczirbel.Secrets
import com.dominiczirbel.network.Spotify
import kotlinx.coroutines.launch

data class AuthenticationViewModel(
    val clientId: String = "",
    val clientSecret: String = "",
    val submitLoading: Boolean = false,
    val loadFromFileLoading: Boolean = false
)

private val PADDING = 10.dp

@Composable
@Suppress("LongMethod")
fun AuthenticationView(
    onAuthenticated: () -> Unit
) {
    val viewModel = remember { mutableStateOf(AuthenticationViewModel()) }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(PADDING), verticalArrangement = Arrangement.spacedBy(PADDING, Alignment.Top)) {
        Text("Enter your Spotify API application credentials.")

        LinkedText(style = MaterialTheme.typography.caption) {
            append("See ")
            appendLinkedUrl(
                text = "the spotify documentation",
                url = "https://developer.spotify.com/documentation/web-api/quick-start/"
            )
            append(" for details.")
        }

        Spacer(Modifier.height(PADDING))

        TextField(
            value = viewModel.value.clientId,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { viewModel.value = viewModel.value.copy(clientId = it) },
            label = {
                Text("Client ID")
            }
        )

        TextField(
            value = viewModel.value.clientSecret,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { viewModel.value = viewModel.value.copy(clientSecret = it) },
            label = {
                Text("Client secret")
            }
        )

        LoadingButton(
            enabled = viewModel.value.clientId.isNotEmpty() && viewModel.value.clientSecret.isNotEmpty(),
            modifier = Modifier.align(Alignment.End),
            loading = viewModel.value.submitLoading,
            onClick = {
                viewModel.value = viewModel.value.copy(submitLoading = true)

                coroutineScope.launch {
                    val result = runCatching {
                        Spotify.authenticate(
                            clientId = viewModel.value.clientId,
                            clientSecret = viewModel.value.clientSecret
                        )
                    }
                    viewModel.value = viewModel.value.copy(submitLoading = false)
                    if (result.isSuccess) {
                        onAuthenticated()
                    }
                }
            }
        ) {
            Text("Submit")
        }

        Spacer(Modifier.height(PADDING))

        LoadingButton(
            loading = viewModel.value.loadFromFileLoading,
            onClick = {
                viewModel.value = viewModel.value.copy(loadFromFileLoading = true)

                coroutineScope.launch {
                    Secrets.load()
                    val result = runCatching { Secrets.authenticate() }
                    viewModel.value = viewModel.value.copy(loadFromFileLoading = false)
                    if (result.isSuccess) {
                        onAuthenticated()
                    }
                }
            }
        ) {
            Text("Load from secrets")
        }
    }
}
