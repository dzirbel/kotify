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
import com.dzirbel.kotify.ui.components.table.Column
import com.dzirbel.kotify.ui.framework.Presenter
import com.dzirbel.kotify.ui.properties.TrackAlbumIndexProperty
import com.dzirbel.kotify.ui.properties.TrackAlbumProperty
import com.dzirbel.kotify.ui.properties.TrackArtistsProperty
import com.dzirbel.kotify.ui.properties.TrackDurationProperty
import com.dzirbel.kotify.ui.properties.TrackNameProperty
import com.dzirbel.kotify.ui.properties.TrackPopularityProperty
import com.dzirbel.kotify.ui.properties.TrackRatingProperty
import com.dzirbel.kotify.ui.properties.TrackSavedProperty
import com.dzirbel.kotify.util.zipToMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

class TracksPresenter(scope: CoroutineScope) : Presenter<TracksPresenter.ViewModel, TracksPresenter.Event>(
    scope = scope,
    startingEvents = listOf(Event.Load(invalidate = false)),
    initialState = ViewModel(),
) {

    data class ViewModel(
        val refreshing: Boolean = false,
        val tracks: ListAdapter<Track> = ListAdapter.empty(),
        val tracksById: Map<String, Track> = emptyMap(),
        val savedTrackIds: Set<String> = emptySet(),
        val trackRatings: Map<String, State<Rating?>> = emptyMap(),
        val tracksUpdated: Long? = null,
    ) {
        val trackProperties: List<Column<Track>> = listOf(
            TrackAlbumIndexProperty,
            TrackSavedProperty(trackIdOf = { it.id.value }, savedTrackIds = savedTrackIds),
            TrackNameProperty,
            TrackArtistsProperty,
            TrackAlbumProperty,
            TrackRatingProperty(trackIdOf = { it.id.value }, trackRatings = trackRatings),
            TrackDurationProperty,
            TrackPopularityProperty,
        )
    }

    sealed class Event {
        data class Load(val invalidate: Boolean) : Event()
        data class ReactToTracksSaved(val trackIds: List<String>, val saved: Boolean) : Event()
    }

    override fun externalEvents(): Flow<Event> {
        return merge(
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
                mutateState { it.copy(refreshing = true) }

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
                        tracks = it.tracks.withElements(tracks),
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
                    val stateTracks = queryState { it.tracksById }.keys

                    val missingTrackIds: List<String> = event.trackIds
                        .minus(stateTracks)

                    if (missingTrackIds.isNotEmpty()) {
                        val missingTracks = fetchTracks(trackIds = missingTrackIds)
                        val missingTracksById = missingTracks.associateBy { it.id.value }

                        mutateState {
                            it.copy(
                                tracksById = it.tracksById.plus(missingTracksById),
                                tracks = it.tracks.plusElements(missingTracks),
                                savedTrackIds = it.savedTrackIds.plus(event.trackIds),
                            )
                        }
                    } else {
                        mutateState {
                            it.copy(savedTrackIds = it.savedTrackIds.plus(event.trackIds))
                        }
                    }
                } else {
                    // if an track has been unsaved, retain the table of tracks but toggle its save state
                    mutateState {
                        it.copy(savedTrackIds = it.savedTrackIds.minus(event.trackIds.toSet()))
                    }
                }
            }
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
