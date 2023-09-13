package com.dzirbel.kotify.repository.episode

import androidx.compose.runtime.Stable
import com.dzirbel.kotify.db.Episode
import com.dzirbel.kotify.db.EpisodeTable
import com.dzirbel.kotify.repository.EntityImageViewModel
import com.dzirbel.kotify.repository.EntityViewModel
import com.dzirbel.kotify.repository.ImageViewModel
import com.dzirbel.kotify.util.CurrentTime
import java.time.Instant

@Stable
data class EpisodeViewModel(
    override val id: String,
    override val name: String,
    override val uri: String? = null,
    override val updatedTime: Instant = CurrentTime.instant,
    override val fullUpdatedTime: Instant? = null,
    val durationMs: Long,
    val description: String,
    val releaseDate: String? = null,
    val images: ImageViewModel = EntityImageViewModel(id, EpisodeTable.EpisodeImageTable.episode),
) : EntityViewModel {

    constructor(episode: Episode) : this(
        id = episode.id.value,
        uri = episode.uri,
        name = episode.name,
        updatedTime = episode.updatedTime,
        fullUpdatedTime = episode.fullUpdatedTime,
        durationMs = episode.durationMs,
        description = episode.description,
        releaseDate = episode.releaseDate,
    )
}
