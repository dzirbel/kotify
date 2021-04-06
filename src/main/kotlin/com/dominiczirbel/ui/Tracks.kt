package com.dominiczirbel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.dominiczirbel.cache.SpotifyCache
import com.dominiczirbel.network.model.Track
import com.dominiczirbel.ui.common.InvalidateButton
import com.dominiczirbel.ui.common.PageStack
import com.dominiczirbel.ui.common.Table
import com.dominiczirbel.ui.theme.Dimens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

private class TracksPresenter(scope: CoroutineScope) :
    Presenter<TracksPresenter.State?, TracksPresenter.Event>(
        scope = scope,
        eventMergeStrategy = EventMergeStrategy.LATEST,
        startingEvents = listOf(Event.Load(invalidate = false)),
        initialState = null
    ) {

    data class State(
        val refreshing: Boolean,
        val tracks: List<Track>,
        val tracksUpdated: Long?
    )

    sealed class Event {
        data class Load(val invalidate: Boolean) : Event()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            is Event.Load -> {
                mutateState { it?.copy(refreshing = true) }

                if (event.invalidate) {
                    SpotifyCache.invalidate(SpotifyCache.GlobalObjects.SavedTracks.ID)
                }

                val tracks = SpotifyCache.Tracks.getSavedTracks()
                    .map { SpotifyCache.Tracks.getTrack(it) }
                    .sortedBy { it.name }

                mutateState {
                    State(
                        refreshing = false,
                        tracks = tracks,
                        tracksUpdated = SpotifyCache.lastUpdated(SpotifyCache.GlobalObjects.SavedTracks.ID)
                    )
                }
            }
        }
    }
}

@Composable
fun BoxScope.Tracks(pageStack: MutableState<PageStack>) {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val presenter = remember { TracksPresenter(scope = scope) }

    ScrollingPage(scrollState = pageStack.value.currentScrollState, state = { presenter.state() }) { state ->
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tracks", fontSize = Dimens.fontTitle)

                Column {
                    InvalidateButton(
                        refreshing = state.refreshing,
                        updated = state.tracksUpdated,
                        onClick = { presenter.emitAsync(TracksPresenter.Event.Load(invalidate = true)) }
                    )
                }
            }

            Spacer(Modifier.height(Dimens.space3))

            Table(
                columns = trackColumns(pageStack),
                items = state.tracks
            )
        }
    }
}
