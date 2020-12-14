package com.dominiczirbel

import androidx.compose.desktop.Window
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.dominiczirbel.network.Spotify
import com.dominiczirbel.network.oauth.AccessToken
import com.dominiczirbel.ui.AuthenticationDialog
import com.dominiczirbel.ui.MainContent
import okhttp3.OkHttpClient
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@ExperimentalTime
fun main() {
    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
            println(">> ${request.method} ${request.url}")

            val (response, duration) = measureTimedValue { chain.proceed(request) }
            println("<< ${response.code} ${response.request.method} ${response.request.url} in $duration")

            response
        }
        .build()

    Spotify.configuration = Spotify.Configuration(
        okHttpClient = okHttpClient,
        oauthOkHttpClient = okHttpClient
    )

    // clear non-refreshable tokens from tests
    AccessToken.Cache.requireRefreshable()

    @Suppress("MagicNumber")
    Window(title = "Spotify Client") {
        MaterialTheme {
            val authenticating = remember { mutableStateOf<Boolean?>(!AccessToken.Cache.hasToken) }
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
