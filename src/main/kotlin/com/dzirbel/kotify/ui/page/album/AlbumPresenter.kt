package com.dzirbel.kotify.ui.page.album

import androidx.compose.runtime.State
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.AlbumRepository
import com.dzirbel.kotify.db.model.SavedAlbumRepository
import com.dzirbel.kotify.db.model.SavedTrackRepository
import com.dzirbel.kotify.db.model.Track
import com.dzirbel.kotify.db.model.TrackRatingRepository
import com.dzirbel.kotify.db.model.TrackRepository
import com.dzirbel.kotify.repository.Rating
import com.dzirbel.kotify.ui.components.adapter.ListAdapter
import com.dzirbel.kotify.ui.components.adapter.Sort
import com.dzirbel.kotify.ui.framework.Presenter
import com.dzirbel.kotify.util.zipToMap
import kotlinx.coroutines.CoroutineScope
import java.time.Instant

class AlbumPresenter(
    private val albumId: String,
    scope: CoroutineScope,
) : Presenter<AlbumPresenter.ViewModel, AlbumPresenter.Event>(
    scope = scope,
    key = albumId,
    startingEvents = listOf(Event.Load(invalidate = false)),
    initialState = ViewModel(),
) {

    data class ViewModel(
        val refreshing: Boolean = false,
        val album: Album? = null,
        val tracks: ListAdapter<Track> = ListAdapter.from(null),
        val totalDurationMs: ULong? = null,
        val savedTracksState: State<Set<String>?>? = null,
        val trackRatings: Map<String, State<Rating?>> = emptyMap(),
        val isSavedState: State<Boolean?>? = null,
        val albumUpdated: Instant? = null,
    )

    sealed class Event {
        data class Load(val invalidate: Boolean) : Event()
        data class SetSort(val sorts: List<Sort<Track>>) : Event()
        data class ToggleSave(val save: Boolean) : Event()
        data class ToggleTrackSaved(val trackId: String, val saved: Boolean) : Event()
        data class RateTrack(val trackId: String, val rating: Rating?) : Event()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            is Event.Load -> {
                mutateState { it.copy(refreshing = true) }

                if (event.invalidate) {
                    AlbumRepository.invalidate(id = albumId)
                }

                val album = AlbumRepository.get(id = albumId) ?: error("TODO show 404 page") // TODO 404 page
                KotifyDatabase.transaction {
                    album.largestImage.loadToCache()
                    album.artists.loadToCache()
                }

                // TODO replace with ListAdapter sorting
                val tracks = album.getAllTracks().sortedBy { it.trackNumber }
                KotifyDatabase.transaction {
                    tracks.onEach { it.artists.loadToCache() }
                }

                val isSavedState = SavedAlbumRepository.savedStateOf(id = albumId, fetchIfUnknown = true)
                val savedTracksState = SavedTrackRepository.libraryState()

                val trackIds = tracks.map { it.id.value }
                val trackRatings = trackIds.zipToMap(TrackRatingRepository.ratingStates(ids = trackIds))

                mutateState {
                    ViewModel(
                        refreshing = false,
                        album = album,
                        tracks = ListAdapter.from(tracks, baseAdapter = it.tracks),
                        totalDurationMs = tracks.sumOf { track -> track.durationMs },
                        savedTracksState = savedTracksState,
                        trackRatings = trackRatings,
                        isSavedState = isSavedState,
                        albumUpdated = album.updatedTime,
                    )
                }

                val fullTracks = TrackRepository.getFull(ids = tracks.map { it.id.value })
                    .zip(tracks) { fullTrack, existingTrack -> fullTrack ?: existingTrack }
                KotifyDatabase.transaction {
                    fullTracks.forEach { it.artists.loadToCache() }
                }

                mutateState {
                    it.copy(
                        tracks = ListAdapter.from(fullTracks, baseAdapter = it.tracks),
                        totalDurationMs = fullTracks.sumOf { track -> track.durationMs },
                    )
                }
            }

            is Event.SetSort -> mutateState {
                it.copy(tracks = it.tracks.withSort(event.sorts))
            }

            is Event.ToggleSave -> SavedAlbumRepository.setSaved(id = albumId, saved = event.save)

            is Event.ToggleTrackSaved -> SavedTrackRepository.setSaved(id = event.trackId, saved = event.saved)

            is Event.RateTrack -> TrackRatingRepository.rate(id = event.trackId, rating = event.rating)
        }
    }
}
