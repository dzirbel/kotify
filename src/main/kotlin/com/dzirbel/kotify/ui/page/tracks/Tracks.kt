package com.dzirbel.kotify.ui.page.tracks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.ui.components.Interpunct
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.PageLoadingSpinner
import com.dzirbel.kotify.ui.components.adapter.DividerSelector
import com.dzirbel.kotify.ui.components.adapter.SortSelector
import com.dzirbel.kotify.ui.components.adapter.dividableProperties
import com.dzirbel.kotify.ui.components.adapter.sortableProperties
import com.dzirbel.kotify.ui.components.table.Table
import com.dzirbel.kotify.ui.theme.Dimens

@Composable
fun TracksPageHeader(presenter: TracksPresenter, state: TracksPresenter.ViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.space5, vertical = Dimens.space4),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("Tracks", style = MaterialTheme.typography.h4)

            if (state.tracks.hasElements) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                        Text(
                            text = "${state.tracks.size} saved artists",
                            modifier = Modifier.padding(end = Dimens.space2),
                        )

                        Interpunct()

                        InvalidateButton(
                            refreshing = state.refreshing,
                            updated = state.tracksUpdated,
                            contentPadding = PaddingValues(all = Dimens.space2),
                            onClick = { presenter.emitAsync(TracksPresenter.Event.Load(invalidate = true)) },
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space3)) {
            DividerSelector(
                dividableProperties = state.trackProperties.dividableProperties(),
                currentDivider = state.tracks.divider,
                onSelectDivider = { presenter.emitAsync(TracksPresenter.Event.SetDivider(divider = it)) },
            )

            SortSelector(
                sortableProperties = state.trackProperties.sortableProperties(),
                sorts = state.tracks.sorts.orEmpty(),
                onSetSort = { presenter.emitAsync(TracksPresenter.Event.SetSorts(sorts = it)) },
            )
        }
    }
}

@Composable
fun TracksPageContent(presenter: TracksPresenter, state: TracksPresenter.ViewModel) {
    if (state.tracks.hasElements) {
        Table(
            columns = state.trackProperties,
            items = state.tracks,
            onSetSort = {
                presenter.emitAsync(TracksPresenter.Event.SetSorts(sorts = listOfNotNull(it)))
            },
        )
    } else {
        PageLoadingSpinner()
    }
}
