package com.dzirbel.kotify.repository.playlist

import com.dzirbel.kotify.db.model.PlaylistTable
import com.dzirbel.kotify.db.model.PlaylistTrack
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.SimplifiedSpotifyEpisode
import com.dzirbel.kotify.network.model.SimplifiedSpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyPlaylistTrack
import com.dzirbel.kotify.network.model.asFlow
import com.dzirbel.kotify.repository.ConvertingRepository
import com.dzirbel.kotify.repository.DatabaseRepository
import com.dzirbel.kotify.repository.Repository
import com.dzirbel.kotify.repository.convertToDB
import com.dzirbel.kotify.repository.episode.EpisodeRepository
import com.dzirbel.kotify.repository.episode.convertToDB
import com.dzirbel.kotify.repository.track.TrackRepository
import com.dzirbel.kotify.repository.user.UserRepository
import com.dzirbel.kotify.repository.user.convertToDB
import com.dzirbel.kotify.repository.util.ReorderCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.sql.update
import java.time.Instant

interface PlaylistTracksRepository :
    Repository<List<PlaylistTrackViewModel>>,
    ConvertingRepository<List<PlaylistTrack>, List<SpotifyPlaylistTrack>> {

    sealed interface PlaylistReorderState {
        /**
         * The first phase in which the set of operations to apply is being calculated.
         */
        data object Calculating : PlaylistReorderState

        /**
         * The second phase in which the reorder operations are being applied to the remote source.
         */
        data class Reordering(val completedOps: Int, val totalOps: Int) : PlaylistReorderState

        /**
         * The third phase in which the track list is being re-fetched to verify the order is correct.
         */
        data object Verifying : PlaylistReorderState
    }

    /**
     * Reorders the given [tracks] for the playlist with the given [playlistId] according to the given [comparator].
     *
     * Reordering only takes place when the returned [Flow] is collected. TODO this is awkward
     */
    fun reorder(
        playlistId: String,
        tracks: List<PlaylistTrackViewModel>,
        comparator: Comparator<PlaylistTrackViewModel>,
    ): Flow<PlaylistReorderState>

    /**
     * Converts the given network model [spotifyPlaylistTrack] to a database model [PlaylistTrack] for the playlist with
     * the given [playlistId] and at the given [index] on the playlist.
     */
    fun convertTrack(
        spotifyPlaylistTrack: SpotifyPlaylistTrack,
        playlistId: String,
        index: Int,
        fetchTime: Instant,
    ): PlaylistTrack?
}

// TODO add CacheStrategy
class DatabasePlaylistTracksRepository(
    scope: CoroutineScope,
    private val trackRepository: TrackRepository,
    private val userRepository: UserRepository,
) :
    DatabaseRepository<List<PlaylistTrackViewModel>, List<PlaylistTrack>, List<SpotifyPlaylistTrack>>(
        entityName = "playlist tracks",
        entityNamePlural = "playlists tracks",
        scope = scope,
    ),
    PlaylistTracksRepository {

    override suspend fun fetchFromRemote(id: String): List<SpotifyPlaylistTrack> {
        return Spotify.Playlists.getPlaylistTracks(playlistId = id).asFlow().toList()
    }

    override fun fetchFromDatabase(id: String): Pair<List<PlaylistTrack>, Instant>? {
        return PlaylistTable.tracksFetchTime(playlistId = id)?.let { fetchTime ->
            val tracks = PlaylistTrack.tracksInOrder(playlistId = id)
            tracks to fetchTime
        }
    }

    override fun convertToDB(
        id: String,
        networkModel: List<SpotifyPlaylistTrack>,
        fetchTime: Instant,
    ): List<PlaylistTrack> {
        PlaylistTable.update(where = { PlaylistTable.id eq id }) {
            it[tracksFetched] = fetchTime
        }

        return networkModel.mapIndexedNotNull { index, spotifyPlaylistTrack ->
            convertTrack(
                spotifyPlaylistTrack = spotifyPlaylistTrack,
                playlistId = id,
                index = index,
                fetchTime = fetchTime,
            )
        }
    }

    override fun convertToVM(databaseModel: List<PlaylistTrack>) = databaseModel.map(::PlaylistTrackViewModel)

    override fun reorder(
        playlistId: String,
        tracks: List<PlaylistTrackViewModel>,
        comparator: Comparator<PlaylistTrackViewModel>,
    ): Flow<PlaylistTracksRepository.PlaylistReorderState> {
        // TODO prevent concurrent reorders of the same playlist
        return flow {
            emit(PlaylistTracksRepository.PlaylistReorderState.Calculating)
            val ops = ReorderCalculator.calculateReorderOperations(list = tracks, comparator = comparator)

            if (ops.isNotEmpty()) {
                emit(PlaylistTracksRepository.PlaylistReorderState.Reordering(completedOps = 0, totalOps = ops.size))

                for ((index, op) in ops.withIndex()) {
                    Spotify.Playlists.reorderPlaylistItems(
                        playlistId = playlistId,
                        rangeStart = op.rangeStart,
                        rangeLength = op.rangeLength,
                        insertBefore = op.insertBefore,
                    )

                    emit(
                        PlaylistTracksRepository.PlaylistReorderState.Reordering(
                            completedOps = index + 1,
                            totalOps = ops.size,
                        ),
                    )
                }

                emit(PlaylistTracksRepository.PlaylistReorderState.Verifying)

                refreshFromRemote(id = playlistId)
                    .join()
            }
        }
    }

    override fun convertTrack(
        spotifyPlaylistTrack: SpotifyPlaylistTrack,
        playlistId: String,
        index: Int,
        fetchTime: Instant,
    ): PlaylistTrack? {
        val playlistTrack = when (val track = spotifyPlaylistTrack.track) {
            is SimplifiedSpotifyTrack -> {
                trackRepository.convertToDB(track, fetchTime)?.id?.value?.let { trackId ->
                    PlaylistTrack.findOrCreateFromTrack(trackId = trackId, playlistId = playlistId)
                }
            }

            is SimplifiedSpotifyEpisode -> {
                val episode = EpisodeRepository.convertToDB(networkModel = track, fetchTime = fetchTime)
                PlaylistTrack.findOrCreateFromEpisode(episodeId = episode.id.value, playlistId = playlistId)
            }

            null -> {
                null
            }

            else -> {
                error("unknown track type: $track")
            }
        }

        return playlistTrack?.apply {
            spotifyPlaylistTrack.addedBy?.let { addedBy = userRepository.convertToDB(it, fetchTime) }
            spotifyPlaylistTrack.addedAt?.let { addedAt = it }
            isLocal = spotifyPlaylistTrack.isLocal
            indexOnPlaylist = index
        }
    }
}
