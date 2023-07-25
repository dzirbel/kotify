package com.dzirbel.kotify.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import com.dzirbel.kotify.network.properties.ShowProperties
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@Tag(TAG_NETWORK)
@ExtendWith(NetworkExtension::class)
class SpotifyShowsTest {
    @ParameterizedTest
    @MethodSource("shows")
    fun getShow(showProperties: ShowProperties) {
        val show = runBlocking { Spotify.Shows.getShow(id = showProperties.id) }

        showProperties.check(show)
    }

    @Test
    fun getShowNotFound() {
        val error = runBlocking {
            assertThrows<Spotify.SpotifyError> { Spotify.Shows.getShow(NetworkFixtures.notFoundId) }
        }

        assertThat(error.code).isEqualTo(404)
    }

    @Test
    fun getShows() {
        val shows = runBlocking { Spotify.Shows.getShows(ids = NetworkFixtures.shows.map { it.id }) }

        shows.zip(NetworkFixtures.shows).forEach { (show, showProperties) -> showProperties.check(show) }
    }

    @ParameterizedTest
    @MethodSource("shows")
    fun getShowEpisodes(showProperties: ShowProperties) {
        val episodes = runBlocking { Spotify.Shows.getShowEpisodes(id = showProperties.id) }

        assertThat(episodes.items).isNotEmpty()
    }

    companion object {
        @JvmStatic
        fun shows() = NetworkFixtures.shows
    }
}
