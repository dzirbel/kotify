package com.dzirbel.kotify.repository.playlist

import androidx.compose.runtime.Stable
import com.dzirbel.kotify.db.model.Playlist
import com.dzirbel.kotify.db.util.largest
import com.dzirbel.kotify.repository.util.LazyTransactionStateFlow

@Stable
class PlaylistViewModel(playlist: Playlist) {
    val id: String = playlist.id.value
    val uri: String? = playlist.uri
    val name: String = playlist.name
    val description: String? = playlist.description
    val followersTotal: Int? = playlist.followersTotal
    val totalTracks: Int? = playlist.totalTracks

    val ownerId: String = playlist.ownerId.value

    val largestImageUrl = LazyTransactionStateFlow("playlist $id largest image") { playlist.images.largest()?.url }
}
