package com.dzirbel.kotify.network

import com.dzirbel.kotify.network.oauth.AccessToken
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.File

class NetworkExtension : BeforeEachCallback, AfterEachCallback, AfterAllCallback {
    override fun beforeEach(context: ExtensionContext) {
        if (context.tags.contains(TAG_NETWORK)) {
            // TODO deduplicate references to access token location
            AccessToken.Cache.cacheFile = File("../.kotify/test-cache/access_token.json")
        } else {
            mockkObject(*spotifyObjects)
        }
    }

    override fun afterEach(context: ExtensionContext) {
        if (!context.tags.contains(TAG_NETWORK)) {
            confirmVerified(*spotifyObjects)
            resetObjectMocks()
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

        // TODO extract
        private fun resetObjectMocks() {
            clearAllMocks(
                answers = false,
                recordedCalls = true,
                childMocks = false,
                regularMocks = false,
                objectMocks = true,
                staticMocks = false,
                constructorMocks = false,
            )
        }
    }
}
