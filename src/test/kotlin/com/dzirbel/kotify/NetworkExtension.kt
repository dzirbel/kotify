package com.dzirbel.kotify

import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.oauth.AccessToken
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * A JUnit test extension which provides a [TestSpotifyInterceptor] for non-network tests.
 *
 * This extension is applied automatically to all tests which are run without the [TAG_NETWORK] tag via a service
 * loader.
 */
class NetworkExtension : BeforeEachCallback, AfterEachCallback, BeforeAllCallback {
    override fun beforeAll(context: ExtensionContext) {
        AccessToken.Cache.cacheFile = Application.cacheDir.resolve("access_token.json")
    }

    override fun beforeEach(context: ExtensionContext) {
        if (!context.tags.contains(TAG_NETWORK)) {
            Spotify.configuration = Spotify.Configuration(
                requestInterceptor = TestSpotifyInterceptor,
            )
        }
    }

    override fun afterEach(context: ExtensionContext) {
        Spotify.configuration = Spotify.Configuration()
        TestSpotifyInterceptor.reset()
        AccessToken.Cache.cacheFile = null
    }
}
