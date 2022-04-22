package com.dzirbel.kotify

import com.dzirbel.kotify.network.Spotify
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * A JUnit test extension which disables [Spotify.allowNetworkCalls] for non-network tests.
 *
 * This extension is applied automatically to all tests via a service loader.
 */
class NetworkExtension : BeforeEachCallback, AfterEachCallback {
    override fun beforeEach(context: ExtensionContext) {
        if (!context.tags.contains(TAG_NETWORK)) {
            Spotify.allowNetworkCalls = false
        }
    }

    override fun afterEach(context: ExtensionContext) {
        Spotify.allowNetworkCalls = true
    }
}
