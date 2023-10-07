package com.dzirbel.kotify

import androidx.compose.ui.window.application
import com.dzirbel.kotify.db.DB
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.log.LogFile
import com.dzirbel.kotify.log.success
import com.dzirbel.kotify.network.DelayInterceptor
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.oauth.AccessToken
import com.dzirbel.kotify.repository.user.DatabaseUserRepository
import com.dzirbel.kotify.ui.IconCache
import com.dzirbel.kotify.ui.SpotifyImageCache
import com.dzirbel.kotify.util.CurrentTime
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import kotlin.time.Duration
import kotlin.time.measureTime

fun main(args: Array<String>) {
    CurrentTime.enabled = true
    val initTimes = mutableListOf<Pair<String, Duration>>()

    fun measureInitTime(name: String, block: () -> Unit) {
        initTimes.add(name to measureTime(block))
    }

    val totalDuration = measureTime {
        measureInitTime("application properties") {
            Application.setup(CLIArguments.parse(args))
        }

        measureInitTime("logs") {
            LogFile.initialize(directory = Application.logDir)
        }

        measureInitTime("settings") {
            Settings.ensureLoaded()
        }

        measureInitTime("database") {
            KotifyDatabase.enabled = true

            KotifyDatabase.init(dbDir = Application.cacheDir, sqlLogger = DatabaseLogger)
            KotifyDatabase.addTransactionListener(DatabaseLogger)

            runBlocking {
                KotifyDatabase[DB.CACHE].transaction("load current user") {
                    DatabaseUserRepository.onConnectToDatabase()
                }
            }
        }

        measureInitTime("network") {
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(NetworkLogger::intercept)
                .addInterceptor(DelayInterceptor)
                .build()

            Spotify.configuration = Spotify.Configuration(
                okHttpClient = okHttpClient,
                oauthOkHttpClient = okHttpClient,
            )

            Spotify.enabled = true
        }

        measureInitTime("access token") {
            AccessToken.Cache.cacheFile = Application.cacheDir.resolve("access_token.json")
            AccessToken.Cache.requireRefreshable() // load access toke and clear non-refreshable tokens from tests
        }

        measureInitTime("image cache") {
            SpotifyImageCache.init(imagesDir = Application.cacheDir.resolve("images"))
        }

        measureInitTime("icon cache") {
            IconCache.init()
        }

        // misc actions with negligible expected runtime
        IconCache.loadBlocking = false
    }

    EventLog.success(
        title = "Application initialized",
        content = initTimes.joinToString(separator = "\n") { (name, duration) -> "$name : $duration" },
        duration = totalDuration,
    )

    application { KotifyWindow() }
}
