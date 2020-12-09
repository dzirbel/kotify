package com.dominiczirbel.network

import com.dominiczirbel.Fixtures
import com.dominiczirbel.ShowProperties
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class SpotifyShowsTest {
    @ParameterizedTest
    @MethodSource("shows")
    fun getShow(showProperties: ShowProperties) {
        val show = runBlocking { Spotify.Shows.getShow(id = showProperties.id) }
        showProperties.check(show)
    }

    @Test
    fun getShows() {
        val shows = runBlocking { Spotify.Shows.getShows(ids = Fixtures.shows.map { it.id }) }
        shows.zip(Fixtures.shows).forEach { it.second.check(it.first) }
    }

    @ParameterizedTest
    @MethodSource("shows")
    fun getShowEpisodes(showProperties: ShowProperties) {
        val episodes = runBlocking { Spotify.Shows.getShowEpisodes(id = showProperties.id) }
        assertThat(episodes.items).isNotEmpty()
    }

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun shows() = Fixtures.shows
    }
}
