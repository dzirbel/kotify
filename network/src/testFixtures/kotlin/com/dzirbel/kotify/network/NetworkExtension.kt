package com.dzirbel.kotify.network

import com.dzirbel.kotify.network.oauth.AccessToken
import com.dzirbel.kotify.util.CurrentTime
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.File

/**
 * A JUnit test extension which sets up the [AccessToken.Cache] and sets [Spotify.enabled] to true.
 */
class NetworkExtension : BeforeEachCallback, AfterEachCallback {
    override fun beforeEach(context: ExtensionContext) {
        AccessToken.Cache.cacheFile = File("../.kotify/test-cache/access_token.json")
        CurrentTime.enabled = true
        Spotify.enabled = true
    }

    override fun afterEach(context: ExtensionContext) {
        Spotify.enabled = false
        CurrentTime.enabled = false
    }
}
