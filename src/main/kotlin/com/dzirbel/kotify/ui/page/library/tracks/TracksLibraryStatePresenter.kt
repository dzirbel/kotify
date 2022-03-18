package com.dzirbel.kotify.ui.page.library.tracks

import com.dzirbel.kotify.db.model.SavedTrackRepository
import com.dzirbel.kotify.db.model.Track
import com.dzirbel.kotify.db.model.TrackRepository
import com.dzirbel.kotify.ui.framework.Presenter
import kotlinx.coroutines.CoroutineScope

class TracksLibraryStatePresenter(scope: CoroutineScope) :
    Presenter<TracksLibraryStatePresenter.ViewModel?, TracksLibraryStatePresenter.Event>(
        scope = scope,
        startingEvents = listOf(Event.Load(fromCache = true)),
        initialState = null
    ) {

    data class ViewModel(
        val savedTrackIds: Set<String>?,

        val tracks: List<Track?>,

        val tracksUpdated: Long?,
        val refreshingSavedTracks: Boolean = false,
    )

    sealed class Event {
        class Load(val fromCache: Boolean) : Event()
        object FetchMissingTracks : Event()
        object InvalidateTracks : Event()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            is Event.Load -> {
                mutateState { it?.copy(refreshingSavedTracks = true) }

                val savedTrackIds = if (event.fromCache) {
                    SavedTrackRepository.getLibraryCached()
                } else {
                    SavedTrackRepository.getLibraryRemote()
                }

                val tracks = TrackRepository.getCached(ids = savedTrackIds?.toList().orEmpty())
                val tracksUpdated = SavedTrackRepository.libraryUpdated()?.toEpochMilli()

                mutateState {
                    ViewModel(
                        savedTrackIds = savedTrackIds,
                        tracks = tracks,
                        tracksUpdated = tracksUpdated,
                        refreshingSavedTracks = false,
                    )
                }
            }

            Event.FetchMissingTracks -> {
                val savedTrackIds = requireNotNull(SavedTrackRepository.getLibraryCached()).toList()

                val tracks = TrackRepository.getFull(ids = savedTrackIds)

                mutateState { it?.copy(tracks = tracks) }
            }

            Event.InvalidateTracks -> {
                val savedTrackIds = requireNotNull(SavedTrackRepository.getLibraryCached()).toList()

                TrackRepository.invalidate(ids = savedTrackIds)

                val tracks = TrackRepository.getCached(ids = savedTrackIds)

                mutateState { it?.copy(tracks = tracks) }
            }
        }
    }
}
