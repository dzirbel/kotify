package com.dzirbel.kotify.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.cache.SavedRepository
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.SavedTrackRepository
import com.dzirbel.kotify.db.model.Track
import com.dzirbel.kotify.db.model.TrackRepository
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.PageStack
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.components.table.Table
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.util.plusSorted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map

private class TracksPresenter(scope: CoroutineScope) :
    Presenter<TracksPresenter.ViewModel?, TracksPresenter.Event>(
        scope = scope,
        eventMergeStrategy = EventMergeStrategy.LATEST,
        startingEvents = listOf(Event.Load(invalidate = false)),
        initialState = null
    ) {

    data class ViewModel(
        val refreshing: Boolean,
        val tracks: List<Track>,
        val savedTrackIds: Set<String>,
        val tracksUpdated: Long?,
    )

    sealed class Event {
        data class Load(val invalidate: Boolean) : Event()
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

                val trackIds = SavedTrackRepository.getLibrary()
                val tracks = fetchTracks(trackIds = trackIds.toList())
                    .sortedBy { it.name }
                val tracksUpdated = SavedTrackRepository.libraryUpdated()

                mutateState {
                    ViewModel(
                        refreshing = false,
                        tracks = tracks,
                        savedTrackIds = trackIds,
                        tracksUpdated = tracksUpdated?.toEpochMilli(),
                    )
                }
            }

            is Event.ReactToTracksSaved -> {
                if (event.saved) {
                    // if an track has been saved but is now missing from the table of tracks, load and add it
                    val stateTracks = queryState { it?.tracks }.orEmpty()

                    val missingTrackIds: List<String> = event.trackIds
                        .minus(stateTracks.mapTo(mutableSetOf()) { it.id.value })

                    if (missingTrackIds.isNotEmpty()) {
                        val missingTracks = fetchTracks(trackIds = missingTrackIds)
                        val allTracks = stateTracks.plusSorted(missingTracks) { it.name }

                        mutateState {
                            it?.copy(tracks = allTracks, savedTrackIds = it.savedTrackIds.plus(event.trackIds))
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
        }
    }

    private suspend fun fetchTracks(trackIds: List<String>): List<Track> {
        val tracks = TrackRepository.get(ids = trackIds).filterNotNull()
        KotifyDatabase.transaction { tracks.forEach { it.album.loadToCache() } }
        return tracks
    }
}

@Composable
fun BoxScope.Tracks(pageStack: MutableState<PageStack>) {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val presenter = remember { TracksPresenter(scope = scope) }

    ScrollingPage(scrollState = pageStack.value.currentScrollState, presenter = presenter) { state ->
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

            VerticalSpacer(Dimens.space3)

            // TODO find the context to play tracks from the list of all saved tracks
            Table(
                columns = trackColumns(
                    pageStack = pageStack,
                    savedTracks = state.savedTrackIds,
                    playContextFromIndex = null,
                ),
                items = state.tracks
            )
        }
    }
}
