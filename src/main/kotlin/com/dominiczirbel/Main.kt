package com.dominiczirbel

import androidx.compose.desktop.AppWindow
import androidx.compose.material.Text
import com.dominiczirbel.cache.SpotifyCache
import com.dominiczirbel.network.DelayInterceptor
import com.dominiczirbel.network.Spotify
import com.dominiczirbel.network.oauth.AccessToken
import com.dominiczirbel.ui.AuthenticationDialog
import com.dominiczirbel.ui.Root
import com.dominiczirbel.ui.theme.Colors
import okhttp3.OkHttpClient
import javax.swing.SwingUtilities

fun main() {
    Logger.logToConsole = true

    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(DelayInterceptor)
        .addInterceptor(Logger.Network::intercept)
        .build()

    Spotify.configuration = Spotify.Configuration(
        okHttpClient = okHttpClient,
        oauthOkHttpClient = okHttpClient
    )

    // clear non-refreshable tokens from tests
    AccessToken.Cache.requireRefreshable()

    SpotifyCache.load()

    SwingUtilities.invokeLater {
        AppWindow(title = "Spotify Client")
            .apply { maximize() }
            .show {
                Colors.current.applyColors {
                    val accessToken = AccessToken.Cache.state()

                    if (accessToken.value == null) {
                        Text("Authenticating...")
                        AuthenticationDialog(
                            onDismissRequest = { },
                            onAuthenticated = { }
                        )
                    } else {
                        Root()
                    }
                }
            }
    }
}
