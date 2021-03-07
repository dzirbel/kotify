package com.dominiczirbel.properties

import com.dominiczirbel.network.model.Episode
import com.google.common.truth.Truth

data class EpisodeProperties(
    override val id: String,
    override val name: String,
    private val description: String,
    private val releaseDate: String,
    private val releaseDatePrecision: String,
    private val languages: List<String>
) : ObjectProperties(type = "episode") {
    fun check(episode: Episode) {
        super.check(episode)

        Truth.assertThat(episode.description).isEqualTo(description)
        Truth.assertThat(episode.durationMs).isAtLeast(0)
        Truth.assertThat(episode.releaseDate).isEqualTo(releaseDate)
        Truth.assertThat(episode.releaseDatePrecision).isEqualTo(releaseDatePrecision)
        Truth.assertThat(episode.languages).isEqualTo(languages)
        Truth.assertThat(episode.isPlayable).isTrue()
    }
}
