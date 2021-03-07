package com.dominiczirbel.network

import com.dominiczirbel.Fixtures
import com.dominiczirbel.TAG_NETWORK
import com.dominiczirbel.TrackProperties
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@Tag(TAG_NETWORK)
internal class SpotifyTracksTest {
    @ParameterizedTest
    @MethodSource("tracks")
    fun getAudioFeatures(trackProperties: TrackProperties) {
        val audioFeatures = runBlocking { Spotify.Tracks.getAudioFeatures(trackProperties.id!!) }

        assertThat(audioFeatures).isNotNull() // TODO more assertions
    }

    @Test
    fun getAudioFeatures() {
        val audioFeatures = runBlocking { Spotify.Tracks.getAudioFeatures(Fixtures.tracks.map { it.id!! }) }

        assertThat(audioFeatures.size).isEqualTo(Fixtures.tracks.size) // TODO more assertions
    }

    @ParameterizedTest
    @MethodSource("tracks")
    fun getAudioAnalysis(trackProperties: TrackProperties) {
        val audioAnalysis = runBlocking { Spotify.Tracks.getAudioAnalysis(trackProperties.id!!) }

        assertThat(audioAnalysis).isNotNull() // TODO more assertions
    }

    @ParameterizedTest
    @MethodSource("tracks")
    fun getTrack(trackProperties: TrackProperties) {
        val track = runBlocking { Spotify.Tracks.getTrack(trackProperties.id!!) }

        trackProperties.check(track)
    }

    @Test
    fun getTrackNotFound() {
        val error = runBlocking {
            assertThrows<Spotify.SpotifyError> { Spotify.Tracks.getTrack(Fixtures.notFoundId) }
        }

        assertThat(error.code).isEqualTo(404)
    }

    @Test
    fun getTracks() {
        val tracks = runBlocking { Spotify.Tracks.getTracks(Fixtures.tracks.map { it.id!! }) }

        tracks.zip(Fixtures.tracks).forEach { (track, trackProperties) -> trackProperties.check(track) }
    }

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun tracks() = Fixtures.tracks
    }
}
