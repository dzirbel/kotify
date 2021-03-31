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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.dominiczirbel.cache.SpotifyCache
import com.dominiczirbel.network.model.Track
import com.dominiczirbel.ui.common.InvalidateButton
import com.dominiczirbel.ui.common.Table
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.ui.util.RemoteState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking

private data class TracksState(
    val tracks: List<Track>,
    val tracksUpdated: Long?
)

@Composable
fun BoxScope.Tracks() {
    val refreshing = remember { mutableStateOf(false) }
    val sharedFlow = remember { MutableSharedFlow<Unit>() }
    val state = RemoteState.of(sharedFlow = sharedFlow) {
        val tracks = SpotifyCache.Tracks.getSavedTracks()
            .map { SpotifyCache.Tracks.getTrack(it) }
            .sortedBy { it.name }

        refreshing.value = false

        TracksState(
            tracks = tracks,
            tracksUpdated = SpotifyCache.lastUpdated(SpotifyCache.GlobalObjects.SavedTracks.ID)
        )
    }

    ScrollingPage(remoteState = state) { tracksState ->
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tracks", fontSize = Dimens.fontTitle)

                Column {
                    InvalidateButton(
                        refreshing = refreshing,
                        updated = tracksState.tracksUpdated,
                        onClick = {
                            SpotifyCache.invalidate(SpotifyCache.GlobalObjects.SavedTracks.ID)
                            runBlocking { sharedFlow.emit(Unit) }
                        }
                    )
                }
            }

            Spacer(Modifier.height(Dimens.space3))

            Table(
                columns = StandardTrackColumns,
                items = tracksState.tracks
            )
        }
    }
}
