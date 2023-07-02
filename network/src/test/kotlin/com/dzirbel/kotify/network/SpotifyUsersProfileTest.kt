package com.dzirbel.kotify.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import com.dzirbel.kotify.util.TAG_NETWORK
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@Tag(TAG_NETWORK)
internal class SpotifyUsersProfileTest {
    @Test
    fun getCurrentUser() {
        val user = runBlocking { Spotify.UsersProfile.getCurrentUser() }

        assertThat(user.displayName).isEqualTo("Test")
        assertThat(user.email).isEqualTo("dominiczirbel+test@gmail.com")
        assertThat(user.id).isEqualTo("34m1o83qloqkyzdt4z3qbveoy")
        assertThat(user.country).isEqualTo("US")
        assertThat(user.product).isEqualTo("free")
        assertThat(user.explicitContent.filterEnabled).isFalse()
        assertThat(user.explicitContent.filterLocked).isFalse()
    }

    @Test
    fun getUser() {
        val user = runBlocking { Spotify.UsersProfile.getUser("34m1o83qloqkyzdt4z3qbveoy") }

        assertThat(user.displayName).isEqualTo("Test")
        assertThat(user.id).isEqualTo("34m1o83qloqkyzdt4z3qbveoy")
    }

    @ParameterizedTest
    @EnumSource(Spotify.UsersProfile.TimeRange::class)
    fun topArtists(timeRange: Spotify.UsersProfile.TimeRange) {
        val artists = runBlocking { Spotify.UsersProfile.topArtists(timeRange = timeRange) }

        // personalization may not have sufficient data for shorter time ranges
        if (timeRange == Spotify.UsersProfile.TimeRange.LONG_TERM) {
            assertThat(artists.items).isNotEmpty()
        }
    }

    @ParameterizedTest
    @EnumSource(Spotify.UsersProfile.TimeRange::class)
    fun topTracks(timeRange: Spotify.UsersProfile.TimeRange) {
        val tracks = runBlocking { Spotify.UsersProfile.topTracks(timeRange = timeRange) }

        // personalization may not have sufficient data for shorter time ranges
        if (timeRange == Spotify.UsersProfile.TimeRange.LONG_TERM) {
            assertThat(tracks.items).isNotEmpty()
        }
    }
}
