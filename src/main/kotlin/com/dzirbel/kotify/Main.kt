package com.dzirbel.kotify

import androidx.compose.desktop.AppWindow
import com.dzirbel.kotify.cache.SpotifyCache
import com.dzirbel.kotify.network.DelayInterceptor
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.oauth.AccessToken
import com.dzirbel.kotify.ui.KeyboardShortcuts
import com.dzirbel.kotify.ui.Root
import com.dzirbel.kotify.ui.theme.Colors
import com.dzirbel.kotify.ui.theme.Dimens
import okhttp3.OkHttpClient
import javax.swing.SwingUtilities

fun main(args: Array<String>) {
    KotifyApplication.setup(
        cachePath = args.getOrNull(0),
        settingsPath = args.getOrNull(1)
    )

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
        AppWindow(title = "Kotify")
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
