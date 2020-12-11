package com.dominiczirbel.network

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class SpotifyPersonalizationTest {
    @ParameterizedTest
    @EnumSource(Spotify.Personalization.TimeRange::class)
    fun topArtists(timeRange: Spotify.Personalization.TimeRange) {
        val artists = runBlocking { Spotify.Personalization.topArtists(timeRange = timeRange) }
        assertThat(artists.items).isNotEmpty()
    }

    @ParameterizedTest
    @EnumSource(Spotify.Personalization.TimeRange::class)
    fun topTracks(timeRange: Spotify.Personalization.TimeRange) {
        val tracks = runBlocking { Spotify.Personalization.topTracks(timeRange = timeRange) }
        assertThat(tracks.items).isNotEmpty()
    }
}
