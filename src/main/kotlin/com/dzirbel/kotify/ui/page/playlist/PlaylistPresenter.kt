package com.dzirbel.kotify.ui.page.playlist

import androidx.compose.runtime.MutableState
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
import com.dzirbel.kotify.ui.components.PageStack
import com.dzirbel.kotify.ui.components.table.Sort
import com.dzirbel.kotify.ui.framework.Presenter
import com.dzirbel.kotify.ui.util.mutate
import com.dzirbel.kotify.util.ReorderCalculator
import com.dzirbel.kotify.util.compareInOrder
import kotlinx.coroutines.CoroutineScope

class PlaylistPresenter(
    private val page: PlaylistPage,
    private val pageStack: MutableState<PageStack>,
    scope: CoroutineScope,
) : Presenter<PlaylistPresenter.ViewModel?, PlaylistPresenter.Event>(
    scope = scope,
    key = page.playlistId,
    startingEvents = listOf(Event.Load(invalidate = false)),
    initialState = null
) {

    data class ViewModel(
        val refreshing: Boolean,
        val reordering: Boolean = false,
        val sorts: List<Sort<PlaylistTrack>> = emptyList(),
        val playlist: Playlist,
        val tracks: List<PlaylistTrack>?,
        val trackRatings: Map<String, State<Rating?>>?,
        val savedTracksState: State<Set<String>?>,
        val isSavedState: State<Boolean?>,
        val playlistUpdated: Long?,
    )

    sealed class Event {
        data class Load(val invalidate: Boolean) : Event()
        data class ToggleSave(val save: Boolean) : Event()
        data class ToggleTrackSaved(val trackId: String, val saved: Boolean) : Event()
        data class RateTrack(val trackId: String, val rating: Rating?) : Event()
        data class SetSorts(val sorts: List<Sort<PlaylistTrack>>) : Event()
        data class Order(val sorts: List<Sort<PlaylistTrack>>, val tracks: List<PlaylistTrack>) : Event()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            is Event.Load -> {
                mutateState { it?.copy(refreshing = true) }

                if (event.invalidate) {
                    PlaylistRepository.invalidate(id = page.playlistId)
                    KotifyDatabase.transaction { PlaylistTrack.invalidate(playlistId = page.playlistId) }
                }

                val playlist = PlaylistRepository.getFull(id = page.playlistId)
                    ?: error("TODO show 404 page") // TODO 404 page
                pageStack.mutate { withPageTitle(title = page.titleFor(playlist)) }
                val playlistUpdated = playlist.updatedTime.toEpochMilli()
                KotifyDatabase.transaction {
                    playlist.owner.loadToCache()
                    playlist.largestImage.loadToCache()
                }

                val isSavedState = SavedPlaylistRepository.savedStateOf(id = playlist.id.value, fetchIfUnknown = true)
                val savedTracksState = SavedTrackRepository.libraryState()

                mutateState {
                    ViewModel(
                        refreshing = false,
                        playlist = playlist,
                        playlistUpdated = playlistUpdated,
                        isSavedState = isSavedState,
                        tracks = null,
                        trackRatings = null,
                        savedTracksState = savedTracksState,
                    )
                }

                val tracks = playlist.getAllTracks()
                loadTracksToCache(tracks)
                val trackRatings = tracks
                    .map { it.track.cached.id.value }
                    .associateWith { trackId -> TrackRatingRepository.ratingState(trackId) }

                mutateState { it?.copy(tracks = tracks, trackRatings = trackRatings) }
            }

            is Event.ToggleSave -> SavedPlaylistRepository.setSaved(id = page.playlistId, saved = event.save)

            is Event.ToggleTrackSaved -> SavedTrackRepository.setSaved(id = event.trackId, saved = event.saved)

            is Event.RateTrack -> TrackRatingRepository.rate(id = event.trackId, rating = event.rating)

            is Event.SetSorts -> mutateState { it?.copy(sorts = event.sorts) }

            is Event.Order -> {
                val ops = ReorderCalculator.calculateReorderOperations(
                    list = event.tracks.withIndex().toList(),
                    comparator = event.sorts.map { it.comparator }.compareInOrder(),
                )

                if (ops.isNotEmpty()) {
                    mutateState { it?.copy(reordering = true) }

                    for (op in ops) {
                        Spotify.Playlists.reorderPlaylistItems(
                            playlistId = page.playlistId,
                            rangeStart = op.rangeStart,
                            rangeLength = op.rangeLength,
                            insertBefore = op.insertBefore,
                        )
                    }

                    KotifyDatabase.transaction { PlaylistTrack.invalidate(playlistId = page.playlistId) }
                    val playlist = PlaylistRepository.getCached(id = page.playlistId)
                    val tracks = playlist?.getAllTracks()
                    tracks?.let { loadTracksToCache(it) }

                    mutateState { it?.copy(tracks = tracks, reordering = false, sorts = emptyList()) }
                }
            }
        }
    }

    private suspend fun loadTracksToCache(tracks: List<PlaylistTrack>) {
        KotifyDatabase.transaction {
            tracks.forEach {
                it.track.loadToCache()
                it.track.cached.artists.loadToCache()
                it.track.cached.album.loadToCache()
            }
        }
    }
}
