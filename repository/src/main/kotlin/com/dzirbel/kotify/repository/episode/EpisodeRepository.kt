package com.dzirbel.kotify.repository.episode

import com.dzirbel.kotify.db.Episode
import com.dzirbel.kotify.db.model.Image
import com.dzirbel.kotify.db.util.sized
import com.dzirbel.kotify.network.model.SpotifyEpisode
import com.dzirbel.kotify.repository.ConvertingRepository
import com.dzirbel.kotify.repository.util.updateOrInsert
import java.time.Instant

/**
 * Wrapper around logic to convert [SpotifyEpisode]s to [Episode]s, despite not being a proper repository.
 */
object EpisodeRepository : ConvertingRepository<Episode, SpotifyEpisode> {
    override fun convertToDB(id: String, networkModel: SpotifyEpisode, fetchTime: Instant): Episode {
        return Episode.updateOrInsert(id = id, networkModel = networkModel, fetchTime = fetchTime) {
            this.durationMs = networkModel.durationMs
            this.description = networkModel.description
            this.releaseDate = networkModel.releaseDate
            this.releaseDatePrecision = networkModel.releaseDatePrecision

            this.images = networkModel.images
                .map { Image.findOrCreate(url = it.url, width = it.width, height = it.height) }
                .sized()
        }
    }
}

fun EpisodeRepository.convertToDB(networkModel: SpotifyEpisode, fetchTime: Instant): Episode {
    return convertToDB(id = networkModel.id, networkModel = networkModel, fetchTime = fetchTime)
}
