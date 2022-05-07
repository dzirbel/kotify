package com.dzirbel.kotify.ui.page.playlist

import androidx.compose.runtime.State
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.Playlist
import com.dzirbel.kotify.db.model.PlaylistRepository
import com.dzirbel.kotify.db.model.PlaylistTrack
import com.dzirbel.kotify.db.model.SavedPlaylistRepository
import com.dzirbel.kotify.db.model.SavedTrackRepository
import com.dzirbel.kotify.db.model.TrackRatingRepository
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.repository.Rating
import com.dzirbel.kotify.ui.components.adapter.ListAdapter
import com.dzirbel.kotify.ui.components.adapter.Sort
import com.dzirbel.kotify.ui.components.adapter.asComparator
import com.dzirbel.kotify.ui.framework.Presenter
import com.dzirbel.kotify.ui.player.Player
import com.dzirbel.kotify.ui.properties.PlaylistTrackAddedAtProperty
import com.dzirbel.kotify.ui.properties.PlaylistTrackIndexProperty
import com.dzirbel.kotify.ui.properties.TrackAlbumProperty
import com.dzirbel.kotify.ui.properties.TrackArtistsProperty
import com.dzirbel.kotify.ui.properties.TrackDurationProperty
import com.dzirbel.kotify.ui.properties.TrackNameProperty
import com.dzirbel.kotify.ui.properties.TrackPlayingColumn
import com.dzirbel.kotify.ui.properties.TrackPopularityProperty
import com.dzirbel.kotify.ui.properties.TrackRatingProperty
import com.dzirbel.kotify.ui.properties.TrackSavedProperty
import com.dzirbel.kotify.util.ReorderCalculator
import com.dzirbel.kotify.util.ignore
import com.dzirbel.kotify.util.zipToMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach

class PlaylistPresenter(
    private val playlistId: String,
    scope: CoroutineScope,
) : Presenter<PlaylistPresenter.ViewModel, PlaylistPresenter.Event>(
    scope = scope,
    key = playlistId,
    startingEvents = listOf(Event.LoadTracks(invalidate = false)),
    initialState = ViewModel(),
) {

    data class ViewModel(
        val refreshing: Boolean = false,
        val refreshingTracks: Boolean = false,
        val reordering: Boolean = false,
        val playlist: Playlist? = null,
        val tracks: ListAdapter<PlaylistTrack> = ListAdapter.empty(defaultSort = PlaylistTrackIndexProperty),
        val trackRatings: Map<String, State<Rating?>> = emptyMap(),
        val savedTracksStates: Map<String, StateFlow<Boolean?>>? = null,
        val isSavedState: State<Boolean?>? = null,
    ) {
        val playlistTrackColumns = listOf(
            TrackPlayingColumn(
                trackIdOf = { it.track.cached.id.value },
                playContextFromTrack = { track ->
                    Player.PlayContext.playlistTrack(
                        playlist = requireNotNull(playlist),
                        index = track.indexOnPlaylist,
                    )
                },
            ),
            PlaylistTrackIndexProperty,
            TrackSavedProperty(
                trackIdOf = { playlistTrack -> playlistTrack.trackId.value },
                savedStateOf = { playlistTrack -> savedTracksStates?.get(playlistTrack.trackId.value) },
            ),
            TrackNameProperty.ForPlaylistTrack,
            TrackArtistsProperty.ForPlaylistTrack,
            TrackAlbumProperty.ForPlaylistTrack,
            TrackRatingProperty(trackIdOf = { it.track.cached.id.value }, trackRatings = trackRatings),
            PlaylistTrackAddedAtProperty,
            TrackDurationProperty.ForPlaylistTrack,
            TrackPopularityProperty.ForPlaylistTrack,
        )
    }

    sealed class Event {
        object RefreshPlaylist : Event()

        data class LoadTracks(val invalidate: Boolean) : Event()
        data class ToggleSave(val save: Boolean) : Event()
        data class SetSorts(val sorts: List<Sort<PlaylistTrack>>) : Event()
        data class Order(val tracks: ListAdapter<PlaylistTrack>) : Event()
    }

    override fun externalEvents(): Flow<Event> {
        return PlaylistRepository.stateOf(
            id = playlistId,
            onStateInitialized = { playlist ->
                if (playlist == null) {
                    throw NotFound("Playlist $playlistId not found")
                } else {
                    mutateState { it.copy(refreshing = false) }
                }
            },
        )
            .onEach { playlist ->
                if (playlist != null) {
                    KotifyDatabase.transaction("load playlist ${playlist.name} owner and image") {
                        playlist.owner.loadToCache()
                        playlist.largestImage.loadToCache()
                    }
                }

                mutateState { it.copy(playlist = playlist) }
            }
            .ignore()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            is Event.RefreshPlaylist -> {
                mutateState { it.copy(refreshing = true) }

                PlaylistRepository.getRemote(id = playlistId)
            }

            is Event.LoadTracks -> {
                if (event.invalidate) {
                    mutateState { it.copy(refreshingTracks = true) }
                }

                val (playlist, tracks) = Playlist.getAllTracks(playlistId = playlistId, allowCache = !event.invalidate)
                loadTracksToCache(playlist, tracks)

                val trackIds = tracks.map { it.track.cached.id.value }
                val trackRatings = trackIds.zipToMap(TrackRatingRepository.ratingStates(ids = trackIds))
                val savedTracksState = trackIds.zipToMap(SavedTrackRepository.statesOf(ids = trackIds))

                mutateState {
                    it.copy(
                        refreshingTracks = false,
                        tracks = it.tracks.withElements(tracks),
                        trackRatings = trackRatings,
                        savedTracksStates = savedTracksState,
                    )
                }
            }

            is Event.ToggleSave -> SavedPlaylistRepository.setSaved(id = playlistId, saved = event.save)

            is Event.SetSorts -> mutateState { it.copy(tracks = it.tracks.withSort(event.sorts)) }

            is Event.Order -> {
                val ops = ReorderCalculator.calculateReorderOperations(
                    list = event.tracks.toList(),
                    comparator = event.tracks.sorts.orEmpty().asComparator(),
                )

                if (ops.isNotEmpty()) {
                    mutateState { it.copy(reordering = true) }

                    for (op in ops) {
                        Spotify.Playlists.reorderPlaylistItems(
                            playlistId = playlistId,
                            rangeStart = op.rangeStart,
                            rangeLength = op.rangeLength,
                            insertBefore = op.insertBefore,
                        )
                    }

                    val (playlist, tracks) = Playlist.getAllTracks(playlistId = playlistId, allowCache = false)
                    loadTracksToCache(playlist, tracks)

                    mutateState {
                        it.copy(
                            tracks = it.tracks.withElements(tracks).withSort(null),
                            reordering = false,
                        )
                    }
                }
            }
        }
    }

    private suspend fun loadTracksToCache(playlist: Playlist?, tracks: List<PlaylistTrack>) {
        KotifyDatabase.transaction("load tracks, tracks artists, and tracks album for playlist ${playlist?.name}") {
            tracks.forEach { playlistTrack ->
                playlistTrack.track.loadToCache()
                playlistTrack.track.cached.artists.loadToCache()
                playlistTrack.track.cached.album.loadToCache()
            }
        }
    }
}
