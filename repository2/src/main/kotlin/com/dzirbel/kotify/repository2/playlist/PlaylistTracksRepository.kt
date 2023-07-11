package com.dzirbel.kotify.repository2.playlist

import com.dzirbel.kotify.db.model.Playlist
import com.dzirbel.kotify.db.model.PlaylistTable
import com.dzirbel.kotify.db.model.PlaylistTrack
import com.dzirbel.kotify.db.model.Track
import com.dzirbel.kotify.db.model.User
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.SpotifyPlaylistTrack
import com.dzirbel.kotify.network.model.asFlow
import com.dzirbel.kotify.repository2.DatabaseRepository
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.sql.update
import java.time.Instant

object PlaylistTracksRepository :
    DatabaseRepository<List<PlaylistTrack>, List<SpotifyPlaylistTrack>>(entityName = "playlist tracks") {

    override suspend fun fetchFromRemote(id: String): List<SpotifyPlaylistTrack> {
        return Spotify.Playlists.getPlaylistTracks(playlistId = id).asFlow().toList()
    }

    override fun fetchFromDatabase(id: String): Pair<List<PlaylistTrack>, Instant>? {
        return Playlist.trackFetchTime(playlistId = id)?.let { fetchTime ->
            Playlist.tracksInOrder(playlistId = id) to fetchTime
        }
    }

    override fun convert(id: String, networkModel: List<SpotifyPlaylistTrack>): List<PlaylistTrack> {
        PlaylistTable.update(where = { PlaylistTable.id eq id }) {
            it[tracksFetched] = Instant.now()
        }

        return networkModel.mapIndexedNotNull { index, spotifyPlaylistTrack ->
            convertTrack(spotifyPlaylistTrack = spotifyPlaylistTrack, playlistId = id, index = index)
        }
    }

    fun convertTrack(spotifyPlaylistTrack: SpotifyPlaylistTrack, playlistId: String, index: Int): PlaylistTrack? {
        return spotifyPlaylistTrack.track
            ?.let { Track.from(it) }
            ?.let { track ->
                PlaylistTrack.recordFor(trackId = track.id.value, playlistId = playlistId).apply {
                    User.from(spotifyPlaylistTrack.addedBy)?.let { addedBy.set(it) }
                    spotifyPlaylistTrack.addedAt?.let { addedAt = it }
                    isLocal = spotifyPlaylistTrack.isLocal
                    indexOnPlaylist = index
                }
            }
    }
}
