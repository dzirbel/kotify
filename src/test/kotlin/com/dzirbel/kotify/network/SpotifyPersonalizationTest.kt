package com.dzirbel.kotify.network

import assertk.assertThat
import assertk.assertions.isNotEmpty
import com.dzirbel.kotify.TAG_NETWORK
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@Tag(TAG_NETWORK)
class SpotifyPersonalizationTest {
    @ParameterizedTest
    @EnumSource(Spotify.Personalization.TimeRange::class)
    fun topArtists(timeRange: Spotify.Personalization.TimeRange) {
        val artists = runBlocking { Spotify.Personalization.topArtists(timeRange = timeRange) }

        // personalization may not have sufficient data for shorter time ranges
        if (timeRange == Spotify.Personalization.TimeRange.LONG_TERM) {
            assertThat(artists.items).isNotEmpty()
        }
    }

    @ParameterizedTest
    @EnumSource(Spotify.Personalization.TimeRange::class)
    fun topTracks(timeRange: Spotify.Personalization.TimeRange) {
        val tracks = runBlocking { Spotify.Personalization.topTracks(timeRange = timeRange) }

        // personalization may not have sufficient data for shorter time ranges
        if (timeRange == Spotify.Personalization.TimeRange.LONG_TERM) {
            assertThat(tracks.items).isNotEmpty()
        }
    }
}
