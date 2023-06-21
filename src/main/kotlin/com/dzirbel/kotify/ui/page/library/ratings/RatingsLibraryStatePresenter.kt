package com.dzirbel.kotify.ui.page.library.ratings

import androidx.compose.runtime.State
import com.dzirbel.kotify.db.model.Track
import com.dzirbel.kotify.db.model.TrackRatingRepository
import com.dzirbel.kotify.db.model.TrackRepository
import com.dzirbel.kotify.repository.Rating
import com.dzirbel.kotify.ui.components.adapter.ListAdapter
import com.dzirbel.kotify.ui.components.adapter.Sort
import com.dzirbel.kotify.ui.framework.Presenter
import com.dzirbel.kotify.util.filterNotNullValues
import com.dzirbel.kotify.util.zipToMap
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.CoroutineScope

class RatingsLibraryStatePresenter(scope: CoroutineScope) :
    Presenter<RatingsLibraryStatePresenter.ViewModel, RatingsLibraryStatePresenter.Event>(
        scope = scope,
        startingEvents = listOf(Event.Load),
        initialState = ViewModel(),
    ) {

    data class ViewModel(
        // ids of the rated tracks
        val ratedTracksIds: ListAdapter<String> = ListAdapter.empty(),

        // map from track id to the track model in the cache; separate from ratedTracks since not all track models might
        // be present in the cache
        val tracks: Map<String, Track> = emptyMap(),

        // map from track id to state of its rating
        val trackRatings: Map<String, State<Rating?>> = emptyMap(),
    )

    sealed class Event {
        object Load : Event()

        object ClearAllRatings : Event()

        class SetSort(val sorts: PersistentList<Sort<String>>) : Event()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            Event.Load -> {
                val ratedTrackIds = TrackRatingRepository.ratedEntities().toList()

                val tracks = ratedTrackIds
                    .zipToMap(TrackRepository.get(ids = ratedTrackIds))
                    .filterNotNullValues()

                val trackRatings = ratedTrackIds
                    .zipToMap(TrackRatingRepository.ratingStates(ids = ratedTrackIds))

                mutateState {
                    ViewModel(
                        ratedTracksIds = it.ratedTracksIds.withElements(ratedTrackIds),
                        tracks = tracks,
                        trackRatings = trackRatings,
                    )
                }
            }

            Event.ClearAllRatings -> {
                TrackRatingRepository.clearAllRatings(userId = null)

                mutateState {
                    ViewModel(
                        ratedTracksIds = it.ratedTracksIds.withElements(emptyList()),
                        tracks = emptyMap(),
                        trackRatings = emptyMap(),
                    )
                }
            }

            is Event.SetSort -> mutateState {
                it.copy(ratedTracksIds = it.ratedTracksIds.withSort(sorts = event.sorts))
            }
        }
    }
}
