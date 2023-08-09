package com.dzirbel.kotify.repository.episode

import com.dzirbel.kotify.db.Episode
import com.dzirbel.kotify.db.model.Image
import com.dzirbel.kotify.db.util.sized
import com.dzirbel.kotify.network.model.SpotifyEpisode
import com.dzirbel.kotify.repository.util.updateOrInsert
import java.time.Instant

/**
 * Wrapper around logic to convert [SpotifyEpisode]s to [Episode]s, despite not being a proper repository.
 */
object EpisodeRepository {
    fun convertToDB(episode: SpotifyEpisode, fetchTime: Instant): Episode {
        return Episode.updateOrInsert(id = episode.id, networkModel = episode, fetchTime = fetchTime) {
            this.durationMs = episode.durationMs
            this.description = episode.description
            this.releaseDate = episode.releaseDate
            this.releaseDatePrecision = episode.releaseDatePrecision

            this.images = episode.images
                .map { Image.findOrCreate(url = it.url, width = it.width, height = it.height) }
                .sized()
        }
    }
}
