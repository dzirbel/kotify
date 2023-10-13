package com.dzirbel.kotify.repository.album

import androidx.compose.runtime.Stable
import com.dzirbel.kotify.repository.track.TrackViewModel
import java.time.Instant

@Stable
data class AlbumTracksViewModel(
    val tracks: List<TrackViewModel>,
    val updateTime: Instant,
)
