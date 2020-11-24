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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dominiczirbel.Secrets
import com.dominiczirbel.network.Spotify
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Composable
@Suppress("LongMethod")
fun AuthenticationView(
    onAuthenticated: () -> Unit
) {
    val clientId = remember { mutableStateOf("") }
    val clientSecret = remember { mutableStateOf("") }
    val submitLoading = remember { mutableStateOf(false) }
    val loadFromFileLoading = remember { mutableStateOf(false) }

    Column {
        TextField(
            value = clientId.value,
            onValueChange = { clientId.value = it },
            label = {
                Text("Client ID")
            }
        )

        TextField(
            value = clientSecret.value,
            onValueChange = { clientSecret.value = it },
            label = {
                Text("Client secret")
            }
        )

        LoadingButton(
            enabled = clientId.value.isNotEmpty() && clientSecret.value.isNotEmpty(),
            loading = submitLoading.value,
            onClick = {
                submitLoading.value = true
                @Suppress("GlobalCoroutineUsage") // TODO find a better way
                GlobalScope.launch {
                    val result = runCatching {
                        Spotify.authenticate(clientId = clientId.value, clientSecret = clientSecret.value)
                    }
                    submitLoading.value = false
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
            loading = loadFromFileLoading.value,
            onClick = {
                loadFromFileLoading.value = true
                @Suppress("GlobalCoroutineUsage") // TODO find a better way
                GlobalScope.launch {
                    Secrets.load()
                    val result = runCatching { Secrets.authenticate() }
                    loadFromFileLoading.value = false
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
