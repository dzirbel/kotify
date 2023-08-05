package com.dzirbel.kotify.repository.playlist

import androidx.compose.runtime.Stable
import com.dzirbel.kotify.db.model.PlaylistTrack
import com.dzirbel.kotify.repository.track.TrackViewModel
import java.time.Instant

@Stable
class PlaylistTrackViewModel(
    playlistTrack: PlaylistTrack,
    val track: TrackViewModel = TrackViewModel(playlistTrack.track),
) {
    val addedAt: String? = playlistTrack.addedAt
    val isLocal: Boolean = playlistTrack.isLocal
    val indexOnPlaylist: Int = playlistTrack.indexOnPlaylist

    val addedAtInstant: Instant? by lazy {
        addedAt?.let { Instant.parse(it) }
    }
}
