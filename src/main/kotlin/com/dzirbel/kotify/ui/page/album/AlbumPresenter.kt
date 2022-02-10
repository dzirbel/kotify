package com.dzirbel.kotify.ui.page.album

import androidx.compose.runtime.MutableState
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
import com.dzirbel.kotify.ui.components.PageStack
import com.dzirbel.kotify.ui.framework.Presenter
import com.dzirbel.kotify.ui.util.mutate
import kotlinx.coroutines.CoroutineScope
import java.time.Instant

class AlbumPresenter(
    private val page: AlbumPage,
    private val pageStack: MutableState<PageStack>,
    scope: CoroutineScope,
) : Presenter<AlbumPresenter.ViewModel?, AlbumPresenter.Event>(
    scope = scope,
    key = page.albumId,
    startingEvents = listOf(Event.Load(invalidate = false)),
    initialState = null,
) {

    data class ViewModel(
        val refreshing: Boolean,
        val album: Album,
        val tracks: List<Track>,
        val savedTracksState: State<Set<String>?>,
        val trackRatings: Map<String, State<Rating?>>,
        val isSavedState: State<Boolean?>,
        val albumUpdated: Instant,
    )

    sealed class Event {
        data class Load(val invalidate: Boolean) : Event()
        data class ToggleSave(val save: Boolean) : Event()
        data class ToggleTrackSaved(val trackId: String, val saved: Boolean) : Event()
        data class RateTrack(val trackId: String, val rating: Rating?) : Event()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            is Event.Load -> {
                mutateState { it?.copy(refreshing = true) }

                if (event.invalidate) {
                    AlbumRepository.invalidate(id = page.albumId)
                }

                val album = AlbumRepository.get(id = page.albumId) ?: error("TODO show 404 page") // TODO 404 page
                KotifyDatabase.transaction {
                    album.largestImage.loadToCache()
                    album.artists.loadToCache()
                }

                pageStack.mutate { withPageTitle(title = page.titleFor(album)) }

                val tracks = album.getAllTracks().sortedBy { it.trackNumber }
                KotifyDatabase.transaction {
                    tracks.onEach { it.artists.loadToCache() }
                }

                val isSavedState = SavedAlbumRepository.savedStateOf(id = page.albumId, fetchIfUnknown = true)
                val savedTracksState = SavedTrackRepository.libraryState()
                val trackRatings = tracks
                    .map { it.id.value }
                    .associateWith { trackId -> TrackRatingRepository.ratingState(trackId) }

                mutateState {
                    ViewModel(
                        refreshing = false,
                        album = album,
                        tracks = tracks,
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

                mutateState { it?.copy(tracks = fullTracks) }
            }

            is Event.ToggleSave -> SavedAlbumRepository.setSaved(id = page.albumId, saved = event.save)

            is Event.ToggleTrackSaved -> SavedTrackRepository.setSaved(id = event.trackId, saved = event.saved)

            is Event.RateTrack -> TrackRatingRepository.rate(id = event.trackId, rating = event.rating)
        }
    }
}
