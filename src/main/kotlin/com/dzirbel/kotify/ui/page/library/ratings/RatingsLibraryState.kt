package com.dzirbel.kotify.ui.page.library.ratings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.components.table.Table
import com.dzirbel.kotify.ui.framework.rememberPresenter
import com.dzirbel.kotify.ui.properties.TrackNameProperty
import com.dzirbel.kotify.ui.properties.TrackRatingProperty
import com.dzirbel.kotify.ui.theme.Dimens

private val RATINGS_TABLE_WIDTH = 750.dp

@Composable
fun RatingsLibraryState() {
    val presenter = rememberPresenter { scope -> RatingsLibraryStatePresenter(scope) }
    val state = presenter.state().stateOrThrow

    val ratingsExpanded = remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${state.ratedTracksIds.size} Rated Tracks", modifier = Modifier.padding(end = Dimens.space3))

            SimpleTextButton(
                onClick = { presenter.emitAsync(RatingsLibraryStatePresenter.Event.ClearAllRatings) }
            ) {
                Text("Clear all ratings")
            }
        }

        SimpleTextButton(onClick = { ratingsExpanded.value = !ratingsExpanded.value }) {
            Text(if (ratingsExpanded.value) "Collapse" else "Expand")
        }
    }

    if (ratingsExpanded.value) {
        Table(
            columns = listOf(
                TrackNameProperty(toTrack = { requireNotNull(state.tracks[it]) }),
                TrackRatingProperty(trackIdOf = { it }, trackRatings = state.trackRatings),
            ),
            items = state.ratedTracksIds,
            modifier = Modifier.widthIn(max = RATINGS_TABLE_WIDTH),
            onSetSort = {
                presenter.emitAsync(RatingsLibraryStatePresenter.Event.SetSort(sorts = listOfNotNull(it)))
            },
        )
    }
}
