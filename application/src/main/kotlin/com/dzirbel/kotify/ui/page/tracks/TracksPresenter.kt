package com.dzirbel.kotify.ui.page.tracks

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.Track
import com.dzirbel.kotify.repository.SavedRepository
import com.dzirbel.kotify.repository.track.SavedTrackRepository
import com.dzirbel.kotify.repository.track.TrackRepository
import com.dzirbel.kotify.repository2.player.Player
import com.dzirbel.kotify.repository2.rating.Rating
import com.dzirbel.kotify.repository2.rating.TrackRatingRepository
import com.dzirbel.kotify.ui.components.adapter.Divider
import com.dzirbel.kotify.ui.components.adapter.ListAdapter
import com.dzirbel.kotify.ui.components.adapter.Sort
import com.dzirbel.kotify.ui.framework.Presenter
import com.dzirbel.kotify.ui.properties.TrackAlbumIndexProperty
import com.dzirbel.kotify.ui.properties.TrackAlbumProperty
import com.dzirbel.kotify.ui.properties.TrackArtistsProperty
import com.dzirbel.kotify.ui.properties.TrackDurationProperty
import com.dzirbel.kotify.ui.properties.TrackNameProperty
import com.dzirbel.kotify.ui.properties.TrackPlayingColumn
import com.dzirbel.kotify.ui.properties.TrackPopularityProperty
import com.dzirbel.kotify.ui.properties.TrackRatingProperty
import com.dzirbel.kotify.ui.properties.TrackSavedProperty
import com.dzirbel.kotify.util.zipToPersistentMap
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
        val tracksById: PersistentMap<String, Track> = persistentMapOf(),
        val savedTrackIds: PersistentSet<String> = persistentSetOf(),
        val trackRatings: ImmutableMap<String, StateFlow<Rating?>> = persistentMapOf(),
        val tracksUpdated: Long? = null,
    ) {
        val trackProperties = persistentListOf(
            TrackPlayingColumn(
                trackIdOf = { it.id.value },
                playContextFromTrack = { Player.PlayContext.track(it) },
            ),
            TrackAlbumIndexProperty,
            TrackSavedProperty(
                trackIdOf = { it.id.value },
                savedStateOf = { MutableStateFlow(savedTrackIds.contains(it.id.value)) },
            ),
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
        data class SetSorts(val sorts: PersistentList<Sort<Track>>) : Event()
        data class SetDivider(val divider: Divider<Track>?) : Event()
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
                val tracksById = tracks.associateBy { it.id.value }.toPersistentMap()
                val tracksUpdated = SavedTrackRepository.libraryUpdated()
                val trackRatings = trackIds.zipToPersistentMap(TrackRatingRepository.ratingStatesOf(ids = trackIds))

                mutateState {
                    ViewModel(
                        refreshing = false,
                        tracks = it.tracks.withElements(tracks),
                        tracksById = tracksById,
                        savedTrackIds = trackIds.toPersistentSet(),
                        trackRatings = trackRatings,
                        tracksUpdated = tracksUpdated?.toEpochMilli(),
                    )
                }
            }

            is Event.ReactToTracksSaved ->
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
                                tracksById = it.tracksById.putAll(missingTracksById),
                                tracks = it.tracks.plusElements(missingTracks),
                                savedTrackIds = it.savedTrackIds.addAll(event.trackIds),
                            )
                        }
                    } else {
                        mutateState {
                            it.copy(savedTrackIds = it.savedTrackIds.addAll(event.trackIds))
                        }
                    }
                } else {
                    // if an track has been unsaved, retain the table of tracks but toggle its save state
                    mutateState {
                        it.copy(savedTrackIds = it.savedTrackIds.removeAll(event.trackIds.toSet()))
                    }
                }

            is Event.SetSorts -> mutateState {
                it.copy(tracks = it.tracks.withSort(event.sorts))
            }

            is Event.SetDivider -> mutateState {
                it.copy(tracks = it.tracks.withDivider(divider = event.divider))
            }
        }
    }

    private suspend fun fetchTracks(trackIds: List<String>): List<Track> {
        val tracks = TrackRepository.get(ids = trackIds).filterNotNull()
        KotifyDatabase.transaction("load tracks album and artists") {
            tracks.forEach { track ->
                track.album.loadToCache()
                track.artists.loadToCache()
            }
        }
        return tracks
    }
}
