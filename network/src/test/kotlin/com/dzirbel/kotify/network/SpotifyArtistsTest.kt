package com.dzirbel.kotify.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import com.dzirbel.kotify.network.model.asFlow
import com.dzirbel.kotify.network.properties.ArtistProperties
import com.dzirbel.kotify.util.zipWithBy
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@Tag(TAG_NETWORK)
internal class SpotifyArtistsTest {
    @ParameterizedTest
    @MethodSource("artists")
    fun getArtist(artistProperties: ArtistProperties) {
        val artist = runBlocking { Spotify.Artists.getArtist(artistProperties.id) }

        artistProperties.check(artist)
    }

    @Test
    fun getArtistNotFound() {
        val error = runBlocking {
            assertThrows<Spotify.SpotifyError> { Spotify.Artists.getArtist(NetworkFixtures.notFoundId) }
        }

        assertThat(error.code).isEqualTo(404)
    }

    @Test
    fun getArtists() {
        val artists = runBlocking { Spotify.Artists.getArtists(NetworkFixtures.artists.map { it.id }) }

        assertThat(artists.size).isEqualTo(NetworkFixtures.artists.size)
        artists.zip(NetworkFixtures.artists).forEach { (artist, artistProperties) -> artistProperties.check(artist) }
    }

    @ParameterizedTest
    @MethodSource("artists")
    fun getArtistAlbums(artistProperties: ArtistProperties) {
        val albums = runBlocking { Spotify.Artists.getArtistAlbums(artistProperties.id).asFlow().toList() }

        if (albums.size != artistProperties.albums.size) {
            val expectedIds = artistProperties.albums.mapTo(mutableSetOf()) { it.id }
            val actualIds = albums.mapTo(mutableSetOf()) { it.id }
            val idToName = artistProperties.albums.associate { it.id to it.name }
                .plus(albums.associate { it.id to it.name })

            val unexpected = expectedIds
                .minus(actualIds)
                .map { id -> Pair(id, idToName[id]) }

            val missing = actualIds
                .minus(expectedIds)
                .map { id -> Pair(id, idToName[id]) }

            if (unexpected.isNotEmpty() || missing.isNotEmpty()) {
                error(
                    """
                        album mismatch for ${artistProperties.name}:
                        ${unexpected.size} expected from response but missing: $unexpected
                        ${missing.size} in response but unexpected: $missing

                        Received:
                        $albums
                    """.trimIndent(),
                )
            }
        }

        albums.zipWithBy(artistProperties.albums) { album, albumProperties -> album.id == albumProperties.id }
            .forEach { (album, albumProperties) -> albumProperties.check(album) }
    }

    @ParameterizedTest
    @MethodSource("artists")
    fun getArtistTopTracks(artistProperties: ArtistProperties) {
        val tracks = runBlocking { Spotify.Artists.getArtistTopTracks(artistProperties.id, market = "US") }

        assertThat(tracks).isNotEmpty()
    }

    @ParameterizedTest
    @MethodSource("artists")
    fun getArtistRelatedArtists(artistProperties: ArtistProperties) {
        val relatedArtists = runBlocking { Spotify.Artists.getArtistRelatedArtists(artistProperties.id) }

        assertThat(relatedArtists).isNotEmpty()
    }

    companion object {
        @JvmStatic
        fun artists() = NetworkFixtures.artists
    }
}
