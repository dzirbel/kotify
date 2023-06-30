package com.dzirbel.kotify.network

import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * A JUnit test extension which provides a [TestSpotifyInterceptor] for non-network tests.
 *
 * This extension is applied automatically to all tests which are run without the [TAG_NETWORK] tag via a service
 * loader.
 */
class NetworkExtension : BeforeEachCallback, AfterEachCallback {
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
    }
}
