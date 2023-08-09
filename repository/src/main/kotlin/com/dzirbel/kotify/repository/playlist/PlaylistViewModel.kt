package com.dzirbel.kotify.repository.playlist

import androidx.compose.runtime.Stable
import com.dzirbel.kotify.db.model.Playlist
import com.dzirbel.kotify.db.model.PlaylistTable
import com.dzirbel.kotify.repository.EntityImageViewModel
import com.dzirbel.kotify.repository.EntityViewModel
import com.dzirbel.kotify.repository.ImageViewModel

@Stable
class PlaylistViewModel(playlist: Playlist) :
    EntityViewModel(playlist),
    ImageViewModel by EntityImageViewModel(playlist, PlaylistTable, PlaylistTable.PlaylistImageTable.playlist) {

    val description: String? = playlist.description
    val followersTotal: Int? = playlist.followersTotal
    val totalTracks: Int? = playlist.totalTracks

    val ownerId: String = playlist.ownerId.value
}
