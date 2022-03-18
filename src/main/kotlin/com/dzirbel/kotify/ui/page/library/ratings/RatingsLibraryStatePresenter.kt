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
import kotlinx.coroutines.CoroutineScope

class RatingsLibraryStatePresenter(scope: CoroutineScope) :
    Presenter<RatingsLibraryStatePresenter.ViewModel?, RatingsLibraryStatePresenter.Event>(
        scope = scope,
        startingEvents = listOf(Event.Load),
        initialState = null
    ) {

    data class ViewModel(
        val ratedTracks: ListAdapter<String>,
        val tracks: Map<String, Track>,
        val trackRatings: Map<String, State<Rating?>>,
    )

    sealed class Event {
        object Load : Event()

        object ClearAllRatings : Event()
        data class RateTrack(val trackId: String, val rating: Rating?) : Event()

        class SetSort(val sorts: List<Sort<String>>) : Event()
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
                        ratedTracks = ListAdapter.from(elements = ratedTrackIds),
                        tracks = tracks,
                        trackRatings = trackRatings,
                    )
                }
            }

            Event.ClearAllRatings -> {
                TrackRatingRepository.clearAllRatings(userId = null)

                mutateState {
                    ViewModel(
                        ratedTracks = ListAdapter.from(elements = emptyList()),
                        tracks = emptyMap(),
                        trackRatings = emptyMap(),
                    )
                }
            }

            is Event.RateTrack -> TrackRatingRepository.rate(id = event.trackId, rating = event.rating)

            is Event.SetSort -> mutateState {
                it?.copy(ratedTracks = it.ratedTracks.withSort(sorts = event.sorts))
            }
        }
    }
}
