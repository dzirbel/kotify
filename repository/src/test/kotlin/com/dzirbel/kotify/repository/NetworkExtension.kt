package com.dzirbel.kotify.repository

import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.util.TAG_NETWORK
import io.mockk.confirmVerified
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

@Suppress("SpreadOperator")
class NetworkExtension : BeforeAllCallback, AfterEachCallback, AfterAllCallback {
    override fun beforeAll(context: ExtensionContext) {
        if (!context.tags.contains(TAG_NETWORK)) {
            mockkObject(*spotifyObjects)
        }
    }

    override fun afterEach(context: ExtensionContext) {
        if (!context.tags.contains(TAG_NETWORK)) {
            confirmVerified(*spotifyObjects)
        }
    }

    override fun afterAll(context: ExtensionContext) {
        if (!context.tags.contains(TAG_NETWORK)) {
            unmockkObject(*spotifyObjects)
        }
    }

    companion object {
        // TODO finish the list
        private val spotifyObjects = arrayOf(
            Spotify,
            Spotify.Player,
        )
    }
}
