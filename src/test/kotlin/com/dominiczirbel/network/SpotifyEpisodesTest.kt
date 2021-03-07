package com.dominiczirbel.network

import com.dominiczirbel.properties.EpisodeProperties
import com.dominiczirbel.Fixtures
import com.dominiczirbel.TAG_NETWORK
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@Tag(TAG_NETWORK)
class SpotifyEpisodesTest {
    @ParameterizedTest
    @MethodSource("episodes")
    fun getEpisode(episodeProperties: EpisodeProperties) {
        val episode = runBlocking { Spotify.Episodes.getEpisode(id = episodeProperties.id) }

        episodeProperties.check(episode)
    }

    @Test
    fun getEpisodes() {
        val episodes = runBlocking { Spotify.Episodes.getEpisodes(ids = Fixtures.episodes.map { it.id }) }

        Fixtures.episodes.zip(episodes).forEach { (episodeProperties, episode) -> episodeProperties.check(episode) }
    }

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun episodes(): List<EpisodeProperties> = Fixtures.episodes
    }
}
