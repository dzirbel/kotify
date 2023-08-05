package com.dzirbel.kotify.repository.playlist

import com.dzirbel.kotify.db.model.Playlist
import com.dzirbel.kotify.db.util.LazyTransactionStateFlow
import com.dzirbel.kotify.db.util.largest

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
