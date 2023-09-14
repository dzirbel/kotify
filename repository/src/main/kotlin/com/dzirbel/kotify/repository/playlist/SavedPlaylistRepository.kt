package com.dzirbel.kotify.repository.playlist

import com.dzirbel.kotify.db.model.PlaylistTable
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.SpotifyPlaylist
import com.dzirbel.kotify.network.model.asFlow
import com.dzirbel.kotify.repository.DatabaseSavedRepository
import com.dzirbel.kotify.repository.SavedRepository
import com.dzirbel.kotify.repository.convertToDB
import com.dzirbel.kotify.repository.user.UserRepository
import com.dzirbel.kotify.util.coroutines.mapParallel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.toList
import java.time.Instant

interface SavedPlaylistRepository : SavedRepository

class DatabaseSavedPlaylistRepository(
    scope: CoroutineScope,
    userRepository: UserRepository,
    private val playlistRepository: PlaylistRepository,
) :
    DatabaseSavedRepository<SpotifyPlaylist>(
        savedEntityTable = PlaylistTable.SavedPlaylistsTable,
        scope = scope,
        userRepository = userRepository,
    ),
    SavedPlaylistRepository {

    override suspend fun fetchIsSaved(ids: List<String>): List<Boolean> {
        val userId = userRepository.requireCurrentUserId

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

    override fun convertToDB(savedNetworkType: SpotifyPlaylist, fetchTime: Instant): Pair<String, Instant?> {
        playlistRepository.convertToDB(networkModel = savedNetworkType, fetchTime = fetchTime)
        return savedNetworkType.id to null
    }
}
