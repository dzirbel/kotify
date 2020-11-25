package com.dominiczirbel

import androidx.compose.desktop.Window
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.dominiczirbel.ui.AuthenticationDialog
import com.dominiczirbel.ui.MainContent
import com.github.kittinunf.fuel.core.FuelManager

fun main() {
    FuelManager.instance.addRequestInterceptor { transformer ->
        { request ->
            println(">> ${request.method} ${request.url}")
            transformer(request)
        }
    }

    FuelManager.instance.addResponseInterceptor { transformer ->
        { request, response ->
            println("<< ${response.statusCode} ${request.method} ${response.url}")
            transformer(request, response)
        }
    }

    @Suppress("MagicNumber")
    Window(title = "Spotify Client") {
        MaterialTheme {
            val authenticating = remember { mutableStateOf<Boolean?>(true) }
            if (authenticating.value == true) {
                Text("Authenticating...")
                AuthenticationDialog(
                    onDismissRequest = { authenticating.value = null },
                    onAuthenticated = { authenticating.value = false }
                )
            } else {
                MainContent(
                    authenticating = authenticating.value,
                    onAuthenticate = { authenticating.value = true }
                )
            }
        }
    }
}
