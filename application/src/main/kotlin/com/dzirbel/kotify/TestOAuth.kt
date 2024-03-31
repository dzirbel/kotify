package com.dzirbel.kotify

import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.oauth.AccessToken
import com.dzirbel.kotify.network.oauth.OAuth
import com.dzirbel.kotify.ui.util.openInBrowser
import com.dzirbel.kotify.util.CurrentTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.system.exitProcess

/**
 * Simple script which runs the OAuth flow to create a cached Spotify access token for use in integration tests.
 */
@Suppress("ForbiddenMethodCall")
fun main() {
    CurrentTime.enabled = true
    Spotify.enabled = true
    AccessToken.Cache.cacheFile = File(".kotify/test-cache/access_token.json")

    val oauth = OAuth.start(
        scopes = OAuth.Scope.TEST_SCOPES,
        openAuthorizationUrl = { url ->
            println("Opening OAuth url: $url")
            openInBrowser(url.toUri())
        },
    )

    println("Waiting for OAuth to be granted...")

    runBlocking { oauth.stops.first() }

    val error = oauth.errorFlow.value
    if (error == null) {
        println("Done!")
    } else {
        println("Error!")
        error.printStackTrace()
    }

    exitProcess(0)
}
