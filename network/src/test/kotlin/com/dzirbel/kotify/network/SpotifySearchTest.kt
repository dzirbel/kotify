package com.dzirbel.kotify.network

import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.dzirbel.kotify.util.TAG_NETWORK
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag(TAG_NETWORK)
class SpotifySearchTest {
    @Test
    fun search() {
        val results = runBlocking {
            Spotify.Search.search(
                q = "blackbriar",
                type = listOf("artist,track"),
            )
        }

        assertThat(results.artists).isNotNull()
        assertThat(results.tracks).isNotNull()

        assertThat(results.albums).isNull()
        assertThat(results.playlists).isNull()
        assertThat(results.shows).isNull()
        assertThat(results.episodes).isNull()

        assertThat(results.artists?.items?.any { it.id == "6PXQUX3BYTSVj7LcvviOmI" }).isNotNull().isTrue()
        assertThat(results.tracks?.items?.any { track -> track.artists.any { it.id == "6PXQUX3BYTSVj7LcvviOmI" } })
            .isNotNull()
            .isTrue()
    }
}
