package com.dzirbel.kotify.ui.page.album

import androidx.compose.runtime.Stable
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.Track
import com.dzirbel.kotify.repository.AverageRating
import com.dzirbel.kotify.repository.Player
import com.dzirbel.kotify.repository.Rating
import com.dzirbel.kotify.repository.album.AlbumRepository
import com.dzirbel.kotify.repository.album.SavedAlbumRepository
import com.dzirbel.kotify.repository.track.SavedTrackRepository
import com.dzirbel.kotify.repository.track.TrackRatingRepository
import com.dzirbel.kotify.repository.track.TrackRepository
import com.dzirbel.kotify.ui.components.adapter.ListAdapter
import com.dzirbel.kotify.ui.components.adapter.Sort
import com.dzirbel.kotify.ui.framework.Presenter
import com.dzirbel.kotify.ui.properties.TrackAlbumIndexProperty
import com.dzirbel.kotify.ui.properties.TrackArtistsProperty
import com.dzirbel.kotify.ui.properties.TrackDurationProperty
import com.dzirbel.kotify.ui.properties.TrackNameProperty
import com.dzirbel.kotify.ui.properties.TrackPlayingColumn
import com.dzirbel.kotify.ui.properties.TrackPopularityProperty
import com.dzirbel.kotify.ui.properties.TrackRatingProperty
import com.dzirbel.kotify.ui.properties.TrackSavedProperty
import com.dzirbel.kotify.util.ignore
import com.dzirbel.kotify.util.zipToPersistentMap
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

    @Stable // necessary due to use of Instant and StateFlow
    data class ViewModel(
        val refreshing: Boolean = false,
        val album: Album? = null,
        val tracks: ListAdapter<Track> = ListAdapter.empty(defaultSort = TrackAlbumIndexProperty),
        val totalDurationMs: Long? = null,
        val savedTracksStates: ImmutableMap<String, StateFlow<Boolean?>>? = null,
        val trackRatings: ImmutableMap<String, StateFlow<Rating?>> = persistentMapOf(),
        val averageRating: StateFlow<AverageRating> = MutableStateFlow(AverageRating.empty),
        val isSaved: Boolean? = null,
        val albumUpdatedMs: Instant? = null,
    ) {
        val trackProperties = persistentListOf(
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
        data class SetSort(val sorts: PersistentList<Sort<Track>>) : Event()
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

                val savedTracksState = trackIds.zipToPersistentMap(SavedTrackRepository.statesOf(ids = trackIds))

                val trackRatings = trackIds.zipToPersistentMap(TrackRatingRepository.ratingStates(ids = trackIds))
                val averageRating = TrackRatingRepository.averageRating(ids = trackIds)

                mutateState { state ->
                    state.copy(
                        refreshing = false,
                        album = album,
                        tracks = state.tracks.withElements(tracks),
                        totalDurationMs = tracks.sumOf { track -> track.durationMs },
                        savedTracksStates = savedTracksState,
                        trackRatings = trackRatings,
                        averageRating = averageRating,
                        albumUpdatedMs = album.updatedTime,
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
