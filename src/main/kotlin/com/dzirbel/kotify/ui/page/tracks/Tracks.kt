package com.dzirbel.kotify.ui.page.tracks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.components.table.Table
import com.dzirbel.kotify.ui.components.trackColumns
import com.dzirbel.kotify.ui.framework.ScrollingPage
import com.dzirbel.kotify.ui.pageStack
import com.dzirbel.kotify.ui.theme.Dimens
import kotlinx.coroutines.Dispatchers

@Composable
fun BoxScope.Tracks() {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val presenter = remember { TracksPresenter(scope = scope) }

    ScrollingPage(scrollState = pageStack.value.currentScrollState, presenter = presenter) { state ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.space4),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
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
                savedTracks = state.savedTrackIds,
                onSetTrackSaved = { trackId, saved ->
                    presenter.emitAsync(TracksPresenter.Event.ToggleTrackSaved(trackId = trackId, saved = saved))
                },
                trackRatings = state.trackRatings,
                onRateTrack = { trackId, rating ->
                    presenter.emitAsync(TracksPresenter.Event.RateTrack(trackId = trackId, rating = rating))
                },
                playContextFromIndex = null,
            ),
            items = state.tracks
        )
    }
}
