package com.dzirbel.kotify.repository.episode

import androidx.compose.runtime.Stable
import com.dzirbel.kotify.db.Episode
import com.dzirbel.kotify.db.util.largest
import com.dzirbel.kotify.repository.EntityViewModel
import com.dzirbel.kotify.repository.util.LazyTransactionStateFlow
import com.dzirbel.kotify.repository.util.ReleaseDate

@Stable
class EpisodeViewModel(episode: Episode) : EntityViewModel(episode) {
    val durationMs: Long = episode.durationMs
    val description: String = episode.description

    val releaseDate: String? = episode.releaseDate
    val parsedReleaseDate: ReleaseDate? by lazy {
        releaseDate?.let { ReleaseDate.parse(it) }
    }

    val largestImageUrl = LazyTransactionStateFlow("epsiode $id largest image") { episode.images.largest()?.url }
}
