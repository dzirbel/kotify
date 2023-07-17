package com.dzirbel.kotify.repository2.playlist

import com.dzirbel.kotify.db.model.Playlist
import com.dzirbel.kotify.db.model.PlaylistTable
import com.dzirbel.kotify.db.model.PlaylistTrack
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.SpotifyPlaylistTrack
import com.dzirbel.kotify.network.model.asFlow
import com.dzirbel.kotify.repository2.DatabaseRepository
import com.dzirbel.kotify.repository2.Repository
import com.dzirbel.kotify.repository2.track.TrackRepository
import com.dzirbel.kotify.repository2.user.UserRepository
import com.dzirbel.kotify.repository2.util.ReorderCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.sql.update
import java.time.Instant

open class PlaylistTracksRepository internal constructor(scope: CoroutineScope) :
    DatabaseRepository<List<PlaylistTrack>, List<SpotifyPlaylistTrack>>(entityName = "playlist tracks", scope = scope) {

    sealed interface PlaylistReorderState {
        /**
         * The first phase in which the set of operations to apply is being calculated.
         */
        object Calculating : PlaylistReorderState

        /**
         * The second phase in which the reorder operations are being applied to the remote source.
         */
        data class Reordering(val completedOps: Int, val totalOps: Int) : PlaylistReorderState

        /**
         * The third phase in which the track list is being re-fetched to verify the order is correct.
         */
        object Verifying : PlaylistReorderState
    }

    override suspend fun fetchFromRemote(id: String): List<SpotifyPlaylistTrack> {
        return Spotify.Playlists.getPlaylistTracks(playlistId = id).asFlow().toList()
    }

    override fun fetchFromDatabase(id: String): Pair<List<PlaylistTrack>, Instant>? {
        return Playlist.trackFetchTime(playlistId = id)?.let { fetchTime ->
            // TODO loadToCache()
            Playlist.tracksInOrder(playlistId = id).onEach { playlistTrack ->
                playlistTrack.track.loadToCache()
                playlistTrack.track.cached.artists.loadToCache()
                playlistTrack.track.cached.album.loadToCache()
            } to fetchTime
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

    /**
     * Reorders the given [tracks] for the playlist with the given [playlistId] according to the given [comparator].
     *
     * Reordering only takes place when the returned [Flow] is collected. TODO this is awkward
     */
    fun reorder(
        playlistId: String,
        tracks: List<PlaylistTrack>,
        comparator: Comparator<PlaylistTrack>,
    ): Flow<PlaylistReorderState> {
        // TODO prevent concurrent reorders of the same playlist
        return flow {
            emit(PlaylistReorderState.Calculating)
            val ops = ReorderCalculator.calculateReorderOperations(list = tracks, comparator = comparator)

            if (ops.isNotEmpty()) {
                emit(PlaylistReorderState.Reordering(completedOps = 0, totalOps = ops.size))
                // mutateState { it.copy(reordering = true) }

                for ((index, op) in ops.withIndex()) {
                    Spotify.Playlists.reorderPlaylistItems(
                        playlistId = playlistId,
                        rangeStart = op.rangeStart,
                        rangeLength = op.rangeLength,
                        insertBefore = op.insertBefore,
                    )

                    emit(PlaylistReorderState.Reordering(completedOps = index + 1, totalOps = ops.size))
                }

                emit(PlaylistReorderState.Verifying)

                refreshFromRemote(id = playlistId)
                    .join()
            }
        }
    }

    /**
     * Converts the given network model [spotifyPlaylistTrack] to a database model [PlaylistTrack] for the playlist with
     * the given [playlistId] and at the given [index] on the playlist.
     */
    fun convertTrack(spotifyPlaylistTrack: SpotifyPlaylistTrack, playlistId: String, index: Int): PlaylistTrack? {
        return spotifyPlaylistTrack.track
            ?.let { track -> track.id?.let { trackId -> TrackRepository.convert(trackId, track) } }
            ?.let { track ->
                PlaylistTrack.recordFor(trackId = track.id.value, playlistId = playlistId).apply {
                    spotifyPlaylistTrack.addedBy?.let { addedBy.set(UserRepository.convert(it.id, it)) }
                    spotifyPlaylistTrack.addedAt?.let { addedAt = it }
                    isLocal = spotifyPlaylistTrack.isLocal
                    indexOnPlaylist = index

                    // TODO loadToCache()
                    this.track.loadToCache()
                    this.track.cached.artists.loadToCache()
                    this.track.cached.album.loadToCache()
                }
            }
    }

    companion object : PlaylistTracksRepository(scope = Repository.applicationScope)
}
