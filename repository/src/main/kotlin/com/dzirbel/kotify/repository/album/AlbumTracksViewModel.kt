package com.dzirbel.kotify.repository.album

import com.dzirbel.kotify.repository.track.TrackViewModel
import java.time.Instant

data class AlbumTracksViewModel(
    val tracks: List<TrackViewModel>,
    val updateTime: Instant,
)
