package com.dzirbel.kotify.repository2.playlist

import com.dzirbel.kotify.db.model.Playlist
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.SpotifyPlaylist
import com.dzirbel.kotify.repository2.DatabaseRepository
import com.dzirbel.kotify.repository2.Repository

object PlaylistRepository : Repository<Playlist> by object : DatabaseRepository<Playlist, SpotifyPlaylist>(Playlist) {
    override suspend fun fetch(id: String) = Spotify.Playlists.getPlaylist(playlistId = id)
}
