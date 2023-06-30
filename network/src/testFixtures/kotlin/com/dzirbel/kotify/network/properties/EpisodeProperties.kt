package com.dzirbel.kotify.network.properties

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isTrue
import com.dzirbel.kotify.network.model.SpotifyEpisode

data class EpisodeProperties(
    override val id: String,
    override val name: String,
    private val description: String,
    private val releaseDate: String,
    private val releaseDatePrecision: String,
    private val languages: List<String>? = null,
) : ObjectProperties(type = "episode") {
    fun check(episode: SpotifyEpisode) {
        super.check(episode)

        assertThat(episode.description).isEqualTo(description)
        assertThat(episode.durationMs).isGreaterThanOrEqualTo(0)
        assertThat(episode.releaseDate).isEqualTo(releaseDate)
        assertThat(episode.releaseDatePrecision).isEqualTo(releaseDatePrecision)
        assertThat(episode.isPlayable).isTrue()

        languages?.let { assertThat(episode.languages).isEqualTo(it) }
    }
}
