package com.dominiczirbel.network

import com.dominiczirbel.ArtistProperties
import com.dominiczirbel.Fixtures
import com.dominiczirbel.Secrets
import com.dominiczirbel.TrackProperties
import com.dominiczirbel.assertThrowsInline
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class SpotifyTest {
    @ParameterizedTest
    @MethodSource("artists")
    fun getArtist(artistProperties: ArtistProperties) {
        artistProperties.check(runBlocking { Spotify.getArtist(artistProperties.id) })
    }

    @Test
    fun getArtists() {
        val artists = runBlocking { Spotify.getArtists(Fixtures.artists.map { it.id }) }
        artists.zip(Fixtures.artists).forEach { (artist, artistProperties) -> artistProperties.check(artist) }
    }

    @ParameterizedTest
    @MethodSource("tracks")
    fun getTrack(trackProperties: TrackProperties) {
        trackProperties.check(runBlocking { Spotify.getTrack(trackProperties.id) })
    }

    @Test
    fun getTrackNotFound() {
        val error = runBlocking { assertThrowsInline<Spotify.SpotifyError> { Spotify.getTrack(Fixtures.notFoundId) } }
        assertEquals(404, error.code)
    }

    @Test
    fun getTracks() {
        val tracks = runBlocking { Spotify.getTracks(Fixtures.tracks.map { it.id }) }
        tracks.zip(Fixtures.tracks).forEach { (track, trackProperties) -> trackProperties.check(track) }
    }

    companion object {
        @BeforeAll
        @JvmStatic
        @Suppress("unused")
        fun setup() {
            Secrets.load()
        }

        @JvmStatic
        @Suppress("unused")
        fun artists() = Fixtures.artists

        @JvmStatic
        @Suppress("unused")
        fun tracks() = Fixtures.tracks
    }
}
