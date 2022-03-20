package com.dzirbel.kotify.ui.page.playlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.db.model.PlaylistTrack
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.LoadedImage
import com.dzirbel.kotify.ui.components.PlayButton
import com.dzirbel.kotify.ui.components.ToggleSaveButton
import com.dzirbel.kotify.ui.components.adapter.SortOrder
import com.dzirbel.kotify.ui.components.adapter.SortSelector
import com.dzirbel.kotify.ui.components.adapter.SortableProperty
import com.dzirbel.kotify.ui.components.adapter.compare
import com.dzirbel.kotify.ui.components.table.Column
import com.dzirbel.kotify.ui.components.table.ColumnByNumber
import com.dzirbel.kotify.ui.components.table.ColumnByString
import com.dzirbel.kotify.ui.components.table.Table
import com.dzirbel.kotify.ui.framework.PageLoadingSpinner
import com.dzirbel.kotify.ui.player.Player
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.util.formatDateTime
import java.time.Instant
import java.util.concurrent.TimeUnit

object AddedAtColumn : ColumnByString<PlaylistTrack>(name = "Added") {
    // TODO precompute rather than re-parsing each time this is accessed
    private val PlaylistTrack.addedAtTimestamp
        get() = Instant.parse(addedAt.orEmpty()).toEpochMilli()

    override val sortableProperty = object : SortableProperty<PlaylistTrack>(sortTitle = name) {
        override fun compare(
            sortOrder: SortOrder,
            first: IndexedValue<PlaylistTrack>,
            second: IndexedValue<PlaylistTrack>,
        ): Int {
            return sortOrder.compare(first.value.addedAtTimestamp, second.value.addedAtTimestamp)
        }
    }

    override fun toString(item: PlaylistTrack, index: Int): String {
        return formatDateTime(timestamp = item.addedAtTimestamp, includeTime = false)
    }
}

object PlaylistTrackIndexColumn : ColumnByNumber<PlaylistTrack>(name = "#") {
    // disable sorting by the index column since it is the default order
    override val sortableProperty: SortableProperty<PlaylistTrack>? = null

    override fun toNumber(item: PlaylistTrack, index: Int) = item.indexOnPlaylist.toInt() + 1
}

@Composable
fun PlaylistPageHeader(
    presenter: PlaylistPresenter,
    state: PlaylistPresenter.ViewModel,
    columns: List<Column<PlaylistTrack>>,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(Dimens.space4),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.space4),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LoadedImage(url = state.playlist?.largestImage?.cached?.url)

            if (state.playlist != null) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.space3)) {
                    Text(state.playlist.name, style = MaterialTheme.typography.h5)

                    state.playlist.description
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { Text(it) }

                    Text(
                        "Created by ${state.playlist.owner.cached.name}; " +
                            "${state.playlist.followersTotal} followers"
                    )

                    val totalDurationMins = remember(state.tracks) {
                        TimeUnit.MILLISECONDS.toMinutes(
                            state.tracks.sumOf { it.track.cached.durationMs.toInt() }.toLong()
                        )
                    }

                    Text("${state.playlist.totalTracks} songs, $totalDurationMins min")

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.space3),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ToggleSaveButton(isSaved = state.isSavedState?.value, size = Dimens.iconMedium) {
                            presenter.emitAsync(PlaylistPresenter.Event.ToggleSave(save = it))
                        }

                        PlayButton(context = Player.PlayContext.playlist(state.playlist))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space2)) {
                        SortSelector(
                            sortProperties = columns.mapNotNull { it.sortableProperty },
                            sorts = state.tracks.sorts.orEmpty(),
                            allowEmpty = true,
                            onSetSort = { sorts ->
                                presenter.emitAsync(PlaylistPresenter.Event.SetSorts(sorts = sorts))
                            },
                        )

                        Button(
                            enabled = !state.tracks.sorts.isNullOrEmpty() && !state.reordering,
                            onClick = {
                                presenter.emitAsync(PlaylistPresenter.Event.Order(tracks = state.tracks))
                            },
                        ) {
                            if (state.reordering) {
                                Text("Reordering...")
                            } else {
                                Text("Set current order as playlist order")
                            }
                        }
                    }
                }
            }
        }

        InvalidateButton(
            refreshing = state.refreshing,
            updated = state.playlistUpdated,
            updatedFormat = { "Playlist synced $it" },
            updatedFallback = "Playlist never synced",
            onClick = { presenter.emitAsync(PlaylistPresenter.Event.Load(invalidate = true)) }
        )
    }
}

@Composable
fun PlaylistPageContent(
    presenter: PlaylistPresenter,
    state: PlaylistPresenter.ViewModel,
    columns: List<Column<PlaylistTrack>>,
) {
    if (state.tracks.hasElements) {
        Table(
            columns = columns,
            items = state.tracks,
            onSetSort = { sort ->
                presenter.emitAsync(PlaylistPresenter.Event.SetSorts(sorts = listOfNotNull(sort)))
            },
        )
    } else {
        PageLoadingSpinner()
    }
}
