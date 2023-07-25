package com.dzirbel.kotify.network

import com.dzirbel.kotify.network.oauth.AccessToken
import io.mockk.confirmVerified
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.File

class NetworkExtension : BeforeEachCallback, AfterEachCallback {
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

            // unmock after each test in case another test in the same class does have the network tag
            unmockkObject(*spotifyObjects)
        }
    }

    companion object {
        private val spotifyObjects = arrayOf(
            Spotify,
            Spotify.Albums,
            Spotify.Artists,
            Spotify.Browse,
            Spotify.Episodes,
            Spotify.Follow,
            Spotify.Library,
            Spotify.Player,
            Spotify.Playlists,
            Spotify.Search,
            Spotify.Tracks,
            Spotify.UsersProfile,
        )
    }
}
