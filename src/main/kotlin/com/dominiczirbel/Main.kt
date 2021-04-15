package com.dominiczirbel

import androidx.compose.desktop.AppWindow
import com.dominiczirbel.cache.SpotifyCache
import com.dominiczirbel.network.DelayInterceptor
import com.dominiczirbel.network.Spotify
import com.dominiczirbel.network.oauth.AccessToken
import com.dominiczirbel.ui.KeyboardShortcuts
import com.dominiczirbel.ui.Root
import com.dominiczirbel.ui.theme.Colors
import com.dominiczirbel.ui.theme.Dimens
import okhttp3.OkHttpClient
import javax.swing.SwingUtilities

fun main() {
    Logger.logToConsole = false

    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(Logger.Network::intercept)
        .addInterceptor(DelayInterceptor)
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
            .apply {
                maximize()
                // TODO doesn't appear to have focus immediately
                KeyboardShortcuts.register(this)
            }
            .show {
                Colors.current.applyColors {
                    Dimens.applyDimens {
                        Root()
                    }
                }
            }
    }
}
