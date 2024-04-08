package com.dzirbel.kotify.network

import assertk.assertThat
import assertk.assertions.hasSameSizeAs
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.dzirbel.kotify.network.properties.TrackProperties
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@ExtendWith(NetworkExtension::class)
internal class SpotifyTracksTest {
    @ParameterizedTest
    @MethodSource("tracks")
    fun getAudioFeatures(trackProperties: TrackProperties) {
        val id = requireNotNull(trackProperties.id)
        val audioFeatures = runBlocking { Spotify.Tracks.getAudioFeatures(id) }

        assertThat(audioFeatures).isNotNull()
    }

    @Test
    fun getAudioFeatures() {
        val audioFeatures = runBlocking {
            Spotify.Tracks.getAudioFeatures(NetworkFixtures.tracks.map { requireNotNull(it.id) })
        }

        assertThat(audioFeatures).hasSameSizeAs(NetworkFixtures.tracks)
    }

    @ParameterizedTest
    @MethodSource("tracks")
    fun getAudioAnalysis(trackProperties: TrackProperties) {
        val id = requireNotNull(trackProperties.id)
        val audioAnalysis = runBlocking { Spotify.Tracks.getAudioAnalysis(id) }

        assertThat(audioAnalysis).isNotNull()
    }

    @ParameterizedTest
    @MethodSource("tracks")
    fun getTrack(trackProperties: TrackProperties) {
        val id = requireNotNull(trackProperties.id)
        val track = runBlocking { Spotify.Tracks.getTrack(id) }

        trackProperties.check(track)
    }

    @Test
    fun getTrackNotFound() {
        val error = runBlocking {
            assertThrows<Spotify.SpotifyError> { Spotify.Tracks.getTrack(NetworkFixtures.notFoundId) }
        }

        assertThat(error.code).isEqualTo(404)
    }

    @Test
    fun getTracks() {
        val tracks = runBlocking {
            Spotify.Tracks.getTracks(NetworkFixtures.tracks.map { requireNotNull(it.id) })
        }

        tracks.zip(NetworkFixtures.tracks).forEach { (track, trackProperties) -> trackProperties.check(track) }
    }

    companion object {
        @JvmStatic
        fun tracks() = NetworkFixtures.tracks
    }
}
