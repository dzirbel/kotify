package com.dzirbel.kotify.repository.playlist

import androidx.compose.runtime.Stable
import com.dzirbel.kotify.db.model.Playlist
import com.dzirbel.kotify.db.util.largest
import com.dzirbel.kotify.repository.EntityViewModel
import com.dzirbel.kotify.repository.util.LazyTransactionStateFlow

@Stable
class PlaylistViewModel(playlist: Playlist) : EntityViewModel(playlist) {
    val description: String? = playlist.description
    val followersTotal: Int? = playlist.followersTotal
    val totalTracks: Int? = playlist.totalTracks

    val ownerId: String = playlist.ownerId.value

    val largestImageUrl = LazyTransactionStateFlow("playlist $id largest image") { playlist.images.largest()?.url }
}
