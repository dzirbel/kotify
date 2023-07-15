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
import com.dzirbel.kotify.repository.user.UserRepository
import com.dzirbel.kotify.repository2.player.PlayerRepository
import com.dzirbel.kotify.repository2.savedRepositories2
import com.dzirbel.kotify.ui.IconCache
import com.dzirbel.kotify.ui.KeyboardShortcuts
import com.dzirbel.kotify.ui.Root
import com.dzirbel.kotify.ui.SpotifyImageCache
import okhttp3.OkHttpClient
import kotlin.time.Duration
import kotlin.time.measureTime

fun main(args: Array<String>) {
    val initTimes = mutableListOf<Pair<String, Duration>>()

    fun measureInitTime(name: String, block: () -> Unit) {
        initTimes.add(name to measureTime(block))
    }

    val totalDuration = measureTime {
        measureInitTime("file system") {
            Application.setup(
                cachePath = args.getOrNull(0),
                settingsPath = args.getOrNull(1),
            )
        }

        measureInitTime("settings") {
            Settings.ensureLoaded()
        }

        measureInitTime("database") {
            KotifyDatabase.init(
                dbFile = Application.cacheDir.resolve("cache.db"),
                sqlLogger = Logger.Database,
                onConnect = {
                    UserRepository.currentUserId.loadToCache()
                    com.dzirbel.kotify.repository2.user.UserRepository.onConnectToDatabase()
                },
            )

            KotifyDatabase.addTransactionListener(Logger.Database)
        }

        measureInitTime("network") {
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(Logger.Network::intercept)
                .addInterceptor(DelayInterceptor)
                .build()

            Spotify.configuration = Spotify.Configuration(
                okHttpClient = okHttpClient,
                oauthOkHttpClient = okHttpClient,
            )
        }

        measureInitTime("saved repositories") {
            savedRepositories2.forEach { it.init() }
        }

        measureInitTime("player init") {
            // load initial player state
            PlayerRepository.refreshPlayback()
            PlayerRepository.refreshTrack()
            PlayerRepository.refreshDevices()
        }

        measureInitTime("access token") {
            AccessToken.Cache.cacheFile = Application.cacheDir.resolve("access_token.json")
            AccessToken.Cache.requireRefreshable() // clear non-refreshable tokens from tests
        }

        measureInitTime("image cache") {
            SpotifyImageCache.init(imagesDir = Application.cacheDir.resolve("images"))
        }

        measureInitTime("icon cache") {
            IconCache.loadBlocking = false
        }
    }

    Logger.Events.info(
        title = "Application initialized in $totalDuration",
        content = initTimes.joinToString(separator = "\n") { (name, duration) -> "$name : $duration" },
    )

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "${Application.name} ${Application.version}",
            state = rememberWindowState(placement = WindowPlacement.Maximized, size = DpSize.Unspecified),
            onKeyEvent = KeyboardShortcuts::handle,
            content = { Root() },
        )
    }
}
