package com.dominiczirbel.network

import com.dominiczirbel.AlbumProperties
import com.dominiczirbel.ArtistProperties
import com.dominiczirbel.Fixtures
import com.dominiczirbel.Secrets
import com.dominiczirbel.TrackProperties
import com.dominiczirbel.assertThrowsInline
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class SpotifyTest {
    @ParameterizedTest
    @MethodSource("albums")
    fun getAlbum(albumProperties: AlbumProperties) {
        albumProperties.check(runBlocking { Spotify.Albums.getAlbum(albumProperties.id) })
    }

    @Test
    fun getAlbums() {
        val albums = runBlocking { Spotify.Albums.getAlbums(Fixtures.albums.keys.map { it.id }) }
        assertEquals(Fixtures.albums.size, albums.size)
        albums.zip(Fixtures.albums.keys).forEach { (album, albumProperties) -> albumProperties.check(album) }
    }

    @ParameterizedTest
    @MethodSource("albums")
    fun getAlbumTracks(albumProperties: AlbumProperties) {
        val tracks = runBlocking { Spotify.Albums.getAlbumTracks(albumProperties.id) }
        val trackProperties = Fixtures.albums.getValue(albumProperties)
        assertEquals(trackProperties.size, tracks.items.size)
        tracks.items.zip(trackProperties).forEach { (track, trackProperties) -> trackProperties.check(track) }
    }

    @ParameterizedTest
    @MethodSource("artists")
    fun getArtist(artistProperties: ArtistProperties) {
        artistProperties.check(runBlocking { Spotify.Artists.getArtist(artistProperties.id) })
    }

    @Test
    fun getArtists() {
        val artists = runBlocking { Spotify.Artists.getArtists(Fixtures.artists.map { it.id }) }
        assertEquals(Fixtures.artists.size, artists.size)
        artists.zip(Fixtures.artists).forEach { (artist, artistProperties) -> artistProperties.check(artist) }
    }

    @ParameterizedTest
    @MethodSource("artists")
    fun getArtistAlbums(artistProperties: ArtistProperties) {
        val albums = runBlocking { Spotify.Artists.getArtistAlbums(artistProperties.id) }
        assertEquals(artistProperties.albums.size, albums.items.size)
        albums.items
            .sortedBy { it.id }
            .zip(artistProperties.albums.sortedBy { it.id })
            .forEach { (album, albumProperties) -> albumProperties.check(album) }
    }

    @ParameterizedTest
    @MethodSource("artists")
    fun getArtistTopTracks(artistProperties: ArtistProperties) {
        val tracks = runBlocking { Spotify.Artists.getArtistTopTracks(artistProperties.id, market = "US") }
        assertTrue(tracks.isNotEmpty())
    }

    @ParameterizedTest
    @MethodSource("artists")
    fun getArtistRelatedArtists(artistProperties: ArtistProperties) {
        val relatedArtists = runBlocking { Spotify.Artists.getArtistRelatedArtists(artistProperties.id) }
        assertTrue(relatedArtists.isNotEmpty())
    }

    @ParameterizedTest
    @MethodSource("tracks")
    fun getTrack(trackProperties: TrackProperties) {
        trackProperties.check(runBlocking { Spotify.Tracks.getTrack(trackProperties.id) })
    }

    @Test
    fun getTrackNotFound() {
        val error = runBlocking {
            assertThrowsInline<Spotify.SpotifyError> { Spotify.Tracks.getTrack(Fixtures.notFoundId) }
        }
        assertEquals(404, error.code)
    }

    @Test
    fun getTracks() {
        val tracks = runBlocking { Spotify.Tracks.getTracks(Fixtures.tracks.map { it.id }) }
        tracks.zip(Fixtures.tracks).forEach { (track, trackProperties) -> trackProperties.check(track) }
    }

    companion object {
        @BeforeAll
        @JvmStatic
        @Suppress("unused")
        fun setup() {
            Secrets.load()
            Secrets.authenticate()
        }

        @JvmStatic
        @Suppress("unused")
        fun albums() = Fixtures.albums.keys

        @JvmStatic
        @Suppress("unused")
        fun artists() = Fixtures.artists

        @JvmStatic
        @Suppress("unused")
        fun tracks() = Fixtures.tracks
    }
}
