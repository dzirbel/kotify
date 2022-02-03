package com.dzirbel.kotify

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.network.DelayInterceptor
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.oauth.AccessToken
import com.dzirbel.kotify.ui.KeyboardShortcuts
import com.dzirbel.kotify.ui.Root
import com.dzirbel.kotify.ui.theme.Theme
import okhttp3.OkHttpClient

fun main(args: Array<String>) {
    Application.setup(
        cachePath = args.getOrNull(0),
        settingsPath = args.getOrNull(1)
    )

    Settings.ensureLoaded()

    KotifyDatabase.db // initialize database connection and create schema

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

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "${Application.name} ${Application.version}",
            state = rememberWindowState(placement = WindowPlacement.Maximized, size = DpSize.Unspecified),
            onKeyEvent = KeyboardShortcuts::handle
        ) {
            Theme.apply { Root() }
        }
    }
}
