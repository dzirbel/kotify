package com.dzirbel.kotify

import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.TAG_NETWORK
import com.dzirbel.kotify.network.TestSpotifyInterceptor
import com.dzirbel.kotify.network.oauth.AccessToken
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.File

/**
 * A JUnit test extension which provides a [TestSpotifyInterceptor] for non-network tests.
 *
 * This extension is applied automatically to all tests which are run without the [TAG_NETWORK] tag via a service
 * loader.
 *
 * TODO duplicate in :network
 */
class NetworkExtension : BeforeEachCallback, AfterEachCallback {
    override fun beforeEach(context: ExtensionContext) {
        if (context.tags.contains(TAG_NETWORK)) {
            AccessToken.Cache.cacheFile = File(".kotify/test-cache/access_token.json")
        } else {
            Spotify.configuration = Spotify.Configuration(
                requestInterceptor = TestSpotifyInterceptor,
            )
        }
    }

    override fun afterEach(context: ExtensionContext) {
        Spotify.configuration = Spotify.Configuration()
        TestSpotifyInterceptor.reset()
    }
}
