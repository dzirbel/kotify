package com.dzirbel.kotify.ui.page.library.tracks

import com.dzirbel.kotify.db.model.SavedTrackRepository
import com.dzirbel.kotify.db.model.Track
import com.dzirbel.kotify.db.model.TrackRepository
import com.dzirbel.kotify.ui.framework.Presenter
import kotlinx.coroutines.CoroutineScope

class TracksLibraryStatePresenter(scope: CoroutineScope) :
    Presenter<TracksLibraryStatePresenter.ViewModel?, TracksLibraryStatePresenter.Event>(
        scope = scope,
        startingEvents = listOf(Event.Load),
        initialState = null
    ) {

    data class ViewModel(
        val tracks: List<Pair<String, Track?>>?,
        val tracksUpdated: Long?,
        val refreshingSavedTracks: Boolean = false,
    )

    sealed class Event {
        object Load : Event()
        object RefreshSavedTracks : Event()
        object FetchMissingTracks : Event()
        object InvalidateTracks : Event()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            Event.Load -> {
                val savedTrackIds = SavedTrackRepository.getLibraryCached()?.toList()
                val tracks = savedTrackIds?.zip(TrackRepository.getCached(ids = savedTrackIds))
                val tracksUpdated = SavedTrackRepository.libraryUpdated()?.toEpochMilli()

                mutateState { ViewModel(tracks = tracks, tracksUpdated = tracksUpdated) }
            }

            Event.RefreshSavedTracks -> {
                mutateState { it?.copy(refreshingSavedTracks = true) }

                SavedTrackRepository.invalidateLibrary()

                val savedTrackIds = SavedTrackRepository.getLibrary().toList()
                val tracks = savedTrackIds.zip(TrackRepository.getCached(ids = savedTrackIds))
                val tracksUpdated = SavedTrackRepository.libraryUpdated()?.toEpochMilli()

                mutateState {
                    it?.copy(
                        tracks = tracks,
                        tracksUpdated = tracksUpdated,
                        refreshingSavedTracks = false
                    )
                }
            }

            Event.FetchMissingTracks -> {
                val savedTrackIds = requireNotNull(SavedTrackRepository.getLibraryCached()).toList()

                val tracks = TrackRepository.getFull(ids = savedTrackIds)

                mutateState { it?.copy(tracks = savedTrackIds.zip(tracks)) }
            }

            Event.InvalidateTracks -> {
                val savedTrackIds = requireNotNull(SavedTrackRepository.getLibraryCached()).toList()

                TrackRepository.invalidate(ids = savedTrackIds)
                val tracks = TrackRepository.getCached(ids = savedTrackIds)

                mutateState { it?.copy(tracks = savedTrackIds.zip(tracks)) }
            }
        }
    }
}
