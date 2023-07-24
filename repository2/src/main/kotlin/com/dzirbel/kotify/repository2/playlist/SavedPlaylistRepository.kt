package com.dzirbel.kotify.repository2.playlist

import com.dzirbel.kotify.db.model.PlaylistTable
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.SpotifyPlaylist
import com.dzirbel.kotify.network.model.asFlow
import com.dzirbel.kotify.repository2.DatabaseSavedRepository
import com.dzirbel.kotify.repository2.Repository
import com.dzirbel.kotify.repository2.user.UserRepository
import com.dzirbel.kotify.util.mapParallel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.toList
import java.time.Instant

open class SavedPlaylistRepository internal constructor(scope: CoroutineScope) :
    DatabaseSavedRepository<SpotifyPlaylist>(savedEntityTable = PlaylistTable.SavedPlaylistsTable, scope = scope) {

    override suspend fun fetchIsSaved(ids: List<String>): List<Boolean> {
        val userId = UserRepository.requireCurrentUserId

        return ids.mapParallel { id ->
            Spotify.Follow.isFollowingPlaylist(playlistId = id, userIds = listOf(userId))
                .first()
        }
    }

    override suspend fun pushSaved(ids: List<String>, saved: Boolean) {
        if (saved) {
            ids.mapParallel { id -> Spotify.Follow.followPlaylist(playlistId = id) }
        } else {
            ids.mapParallel { id -> Spotify.Follow.unfollowPlaylist(playlistId = id) }
        }
    }

    override suspend fun fetchLibrary(): Iterable<SpotifyPlaylist> {
        return Spotify.Playlists.getPlaylists(limit = Spotify.MAX_LIMIT).asFlow().toList()
    }

    override fun convert(savedNetworkType: SpotifyPlaylist): Pair<String, Instant?> {
        PlaylistRepository.convert(id = savedNetworkType.id, networkModel = savedNetworkType)
        return savedNetworkType.id to null
    }

    companion object : SavedPlaylistRepository(scope = Repository.userSessionScope)
}
