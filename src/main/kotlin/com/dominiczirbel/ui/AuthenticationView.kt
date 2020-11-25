package com.dominiczirbel.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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

@Composable
@Suppress("LongMethod")
fun AuthenticationView(
    onAuthenticated: () -> Unit
) {
    val viewModel = remember { mutableStateOf(AuthenticationViewModel()) }
    val coroutineScope = rememberCoroutineScope()

    Column {
        TextField(
            value = viewModel.value.clientId,
            onValueChange = { viewModel.value = viewModel.value.copy(clientId = it) },
            label = {
                Text("Client ID")
            }
        )

        TextField(
            value = viewModel.value.clientSecret,
            onValueChange = { viewModel.value = viewModel.value.copy(clientSecret = it) },
            label = {
                Text("Client secret")
            }
        )

        LoadingButton(
            enabled = viewModel.value.clientId.isNotEmpty() && viewModel.value.clientSecret.isNotEmpty(),
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

        @Suppress("MagicNumber")
        Spacer(Modifier.height(20.dp))

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

@Composable
fun LoadingButton(
    enabled: Boolean = true,
    loading: Boolean,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        enabled = enabled,
        onClick = onClick
    ) {
        if (loading) {
            Image(Icons.Filled.Check)
        } else {
            content()
        }
    }
}
