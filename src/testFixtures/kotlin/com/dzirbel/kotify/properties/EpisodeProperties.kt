package com.dzirbel.kotify.properties

import com.dzirbel.kotify.network.model.Episode
import com.google.common.truth.Truth.assertThat

data class EpisodeProperties(
    override val id: String,
    override val name: String,
    private val description: String,
    private val releaseDate: String,
    private val releaseDatePrecision: String,
    private val languages: List<String>? = null,
) : ObjectProperties(type = "episode") {
    fun check(episode: Episode) {
        super.check(episode)

        assertThat(episode.description).isEqualTo(description)
        assertThat(episode.durationMs).isAtLeast(0)
        assertThat(episode.releaseDate).isEqualTo(releaseDate)
        assertThat(episode.releaseDatePrecision).isEqualTo(releaseDatePrecision)
        assertThat(episode.isPlayable).isTrue()

        languages?.let { assertThat(episode.languages).isEqualTo(it) }
    }
}
