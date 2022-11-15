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
import com.dzirbel.kotify.ui.player.Player
import com.dzirbel.kotify.ui.properties.TrackAlbumIndexProperty
import com.dzirbel.kotify.ui.properties.TrackArtistsProperty
import com.dzirbel.kotify.ui.properties.TrackDurationProperty
import com.dzirbel.kotify.ui.properties.TrackNameProperty
import com.dzirbel.kotify.ui.properties.TrackPlayingColumn
import com.dzirbel.kotify.ui.properties.TrackPopularityProperty
import com.dzirbel.kotify.ui.properties.TrackRatingProperty
import com.dzirbel.kotify.ui.properties.TrackSavedProperty
import com.dzirbel.kotify.util.ignore
import com.dzirbel.kotify.util.zipToMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
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
        val tracks: ListAdapter<Track> = ListAdapter.empty(defaultSort = TrackAlbumIndexProperty),
        val totalDurationMs: Long? = null,
        val savedTracksStates: Map<String, StateFlow<Boolean?>>? = null,
        val trackRatings: Map<String, State<Rating?>> = emptyMap(),
        val isSaved: Boolean? = null,
        val albumUpdated: Instant? = null,
    ) {
        val trackProperties = listOf(
            TrackPlayingColumn(
                trackIdOf = { it.id.value },
                playContextFromTrack = { track ->
                    album?.let {
                        Player.PlayContext.albumTrack(album = album, index = track.trackNumber)
                    }
                },
            ),
            TrackAlbumIndexProperty,
            TrackSavedProperty(
                trackIdOf = { track -> track.id.value },
                savedStateOf = { track -> savedTracksStates?.get(track.id.value) },
            ),
            TrackNameProperty,
            TrackArtistsProperty,
            TrackRatingProperty(trackIdOf = { it.id.value }, trackRatings = trackRatings),
            TrackDurationProperty,
            TrackPopularityProperty,
        )
    }

    sealed class Event {
        data class Load(val invalidate: Boolean) : Event()
        data class SetSort(val sorts: List<Sort<Track>>) : Event()
        data class ToggleSave(val save: Boolean) : Event()
    }

    override fun externalEvents(): Flow<Event> {
        return SavedAlbumRepository.stateOf(id = albumId)
            .onEach { saved ->
                mutateState { it.copy(isSaved = saved) }
            }
            .ignore()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            is Event.Load -> {
                mutateState { it.copy(refreshing = true) }

                val album = AlbumRepository.get(id = albumId, allowCache = !event.invalidate)
                    ?: throw NotFound("Album $albumId not found")

                KotifyDatabase.transaction("load album ${album.name} image and artists") {
                    album.largestImage.loadToCache()
                    album.artists.loadToCache()
                }

                val tracks = album.getAllTracks()
                KotifyDatabase.transaction("load album ${album.name} tracks artists") {
                    tracks.onEach { it.artists.loadToCache() }
                }

                val trackIds = tracks.map { it.id.value }

                val savedTracksState = trackIds.zipToMap(SavedTrackRepository.statesOf(ids = trackIds))

                val trackRatings = trackIds.zipToMap(TrackRatingRepository.ratingStates(ids = trackIds))

                mutateState { state ->
                    state.copy(
                        refreshing = false,
                        album = album,
                        tracks = state.tracks.withElements(tracks),
                        totalDurationMs = tracks.sumOf { track -> track.durationMs },
                        savedTracksStates = savedTracksState,
                        trackRatings = trackRatings,
                        albumUpdated = album.updatedTime,
                    )
                }

                val fullTracks = TrackRepository.getFull(ids = tracks.map { it.id.value })
                    .zip(tracks) { fullTrack, existingTrack -> fullTrack ?: existingTrack }
                KotifyDatabase.transaction("load album ${album.name} full-tracks artists") {
                    fullTracks.forEach { it.artists.loadToCache() }
                }

                mutateState { state ->
                    state.copy(
                        tracks = state.tracks.withElements(fullTracks),
                        totalDurationMs = fullTracks.sumOf { track -> track.durationMs },
                    )
                }
            }

            is Event.SetSort -> mutateState {
                it.copy(tracks = it.tracks.withSort(event.sorts))
            }

            is Event.ToggleSave -> SavedAlbumRepository.setSaved(id = albumId, saved = event.save)
        }
    }
}
