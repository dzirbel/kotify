package com.dominiczirbel.network

import com.dominiczirbel.ArtistProperties
import com.dominiczirbel.Fixtures
import com.dominiczirbel.network.model.SimplifiedAlbum
import com.dominiczirbel.zipWithBy
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

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
            assertThrows<Spotify.SpotifyError> { Spotify.Artists.getArtist(Fixtures.notFoundId) }
        }

        assertThat(error.code).isEqualTo(404)
    }

    @Test
    fun getArtists() {
        val artists = runBlocking { Spotify.Artists.getArtists(Fixtures.artists.map { it.id }) }

        assertThat(artists.size).isEqualTo(Fixtures.artists.size)
        artists.zip(Fixtures.artists).forEach { (artist, artistProperties) -> artistProperties.check(artist) }
    }

    @ParameterizedTest
    @MethodSource("artists")
    fun getArtistAlbums(artistProperties: ArtistProperties) {
        val albums = runBlocking { Spotify.Artists.getArtistAlbums(artistProperties.id).fetchAll<SimplifiedAlbum>() }

        albums.zipWithBy(artistProperties.albums) { album, albumProperties -> album.id == albumProperties.id }
            .forEach { (album, albumProperties) -> albumProperties.check(album) }
    }

    @ParameterizedTest
    @MethodSource("artists")
    fun getArtistTopTracks(artistProperties: ArtistProperties) {
        val tracks = runBlocking { Spotify.Artists.getArtistTopTracks(artistProperties.id, country = "US") }

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
        @Suppress("unused")
        fun artists() = Fixtures.artists
    }
}
