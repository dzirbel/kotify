package com.dzirbel.kotify.ui.page.tracks

import androidx.compose.runtime.State
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.SavedTrackRepository
import com.dzirbel.kotify.db.model.Track
import com.dzirbel.kotify.db.model.TrackRatingRepository
import com.dzirbel.kotify.db.model.TrackRepository
import com.dzirbel.kotify.repository.Rating
import com.dzirbel.kotify.repository.SavedRepository
import com.dzirbel.kotify.ui.components.adapter.ListAdapter
import com.dzirbel.kotify.ui.framework.Presenter
import com.dzirbel.kotify.util.zipToMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map

class TracksPresenter(scope: CoroutineScope) : Presenter<TracksPresenter.ViewModel?, TracksPresenter.Event>(
    scope = scope,
    startingEvents = listOf(Event.Load(invalidate = false)),
    initialState = null
) {

    data class ViewModel(
        val refreshing: Boolean,
        val tracks: ListAdapter<Track>,
        val tracksById: Map<String, Track>,
        val savedTrackIds: Set<String>,
        val trackRatings: Map<String, State<Rating?>>?,
        val tracksUpdated: Long?,
    )

    sealed class Event {
        data class Load(val invalidate: Boolean) : Event()
        data class ToggleTrackSaved(val trackId: String, val saved: Boolean) : Event()
        data class RateTrack(val trackId: String, val rating: Rating?) : Event()
        data class ReactToTracksSaved(val trackIds: List<String>, val saved: Boolean) : Event()
    }

    override fun eventFlows(): Iterable<Flow<Event>> {
        return listOf(
            SavedTrackRepository.eventsFlow()
                .filterIsInstance<SavedRepository.Event.SetSaved>()
                .map { Event.ReactToTracksSaved(trackIds = it.ids, saved = it.saved) },

            SavedTrackRepository.eventsFlow()
                .filterIsInstance<SavedRepository.Event.QueryLibraryRemote>()
                .map { Event.Load(invalidate = false) },
        )
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            is Event.Load -> {
                mutateState { it?.copy(refreshing = true) }

                if (event.invalidate) {
                    SavedTrackRepository.invalidateLibrary()
                }

                val trackIds = SavedTrackRepository.getLibrary().toList()
                val tracks = fetchTracks(trackIds = trackIds)
                val tracksById = tracks.associateBy { it.id.value }
                val tracksUpdated = SavedTrackRepository.libraryUpdated()
                val trackRatings = KotifyDatabase.transaction {
                    trackIds.zipToMap(TrackRatingRepository.ratingStates(ids = trackIds))
                }

                mutateState {
                    ViewModel(
                        refreshing = false,
                        tracks = ListAdapter.from(tracks, baseAdapter = it?.tracks),
                        tracksById = tracksById,
                        savedTrackIds = trackIds.toSet(),
                        trackRatings = trackRatings,
                        tracksUpdated = tracksUpdated?.toEpochMilli(),
                    )
                }
            }

            is Event.ReactToTracksSaved -> {
                if (event.saved) {
                    // if an track has been saved but is now missing from the table of tracks, load and add it
                    val stateTracks = queryState { it?.tracksById }?.keys.orEmpty()

                    val missingTrackIds: List<String> = event.trackIds
                        .minus(stateTracks)

                    if (missingTrackIds.isNotEmpty()) {
                        val missingTracks = fetchTracks(trackIds = missingTrackIds)
                        val missingTracksById = missingTracks.associateBy { it.id.value }

                        mutateState {
                            it?.copy(
                                tracksById = it.tracksById.plus(missingTracksById),
                                tracks = it.tracks.plusElements(missingTracks),
                                savedTrackIds = it.savedTrackIds.plus(event.trackIds),
                            )
                        }
                    } else {
                        mutateState {
                            it?.copy(savedTrackIds = it.savedTrackIds.plus(event.trackIds))
                        }
                    }
                } else {
                    // if an track has been unsaved, retain the table of tracks but toggle its save state
                    mutateState {
                        it?.copy(savedTrackIds = it.savedTrackIds.minus(event.trackIds.toSet()))
                    }
                }
            }

            is Event.ToggleTrackSaved -> SavedTrackRepository.setSaved(id = event.trackId, saved = event.saved)

            is Event.RateTrack -> TrackRatingRepository.rate(id = event.trackId, rating = event.rating)
        }
    }

    private suspend fun fetchTracks(trackIds: List<String>): List<Track> {
        val tracks = TrackRepository.get(ids = trackIds).filterNotNull()
        KotifyDatabase.transaction {
            tracks.forEach { track ->
                track.album.loadToCache()
                track.artists.loadToCache()
            }
        }
        return tracks
    }
}
