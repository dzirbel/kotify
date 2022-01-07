package com.dzirbel.kotify.db.model

import com.dzirbel.kotify.DatabaseExtension
import com.dzirbel.kotify.network.model.SimplifiedSpotifyArtist
import com.dzirbel.kotify.network.model.SimplifiedSpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyExternalUrl
import com.google.common.truth.Truth.assertThat
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(DatabaseExtension::class)
internal class TrackTest {
    @Test
    fun testFromSimplified() {
        val simplifiedSpotifyTrack = SimplifiedSpotifyTrack(
            id = "id1",
            artists = listOf(
                SimplifiedSpotifyArtist(
                    id = "artist id 1",
                    externalUrls = SpotifyExternalUrl(),
                    name = "artist 1",
                    type = "artist",
                ),
                SimplifiedSpotifyArtist(
                    id = "artist id 2",
                    externalUrls = SpotifyExternalUrl(),
                    name = "artist 2",
                    type = "artist",
                ),
            ),
            discNumber = 1,
            durationMs = 2,
            explicit = true,
            externalUrls = SpotifyExternalUrl(),
            isLocal = false,
            name = "name",
            trackNumber = 3,
            type = "track",
        )

        val track = transaction { Track.from(simplifiedSpotifyTrack) }

        requireNotNull(track)
        assertThat(track.id.value).isEqualTo(simplifiedSpotifyTrack.id)
        assertThat(track.name).isEqualTo(simplifiedSpotifyTrack.name)

        val artists = transaction { track.artists.live.map { it.name } }
        assertThat(artists).containsExactly("artist 1", "artist 2")
    }
}
