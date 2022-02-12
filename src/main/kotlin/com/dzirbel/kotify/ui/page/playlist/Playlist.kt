package com.dzirbel.kotify.ui.page.playlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.db.model.PlaylistTrack
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.LoadedImage
import com.dzirbel.kotify.ui.components.PlayButton
import com.dzirbel.kotify.ui.components.ToggleSaveButton
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.components.table.ColumnByString
import com.dzirbel.kotify.ui.components.table.IndexColumn
import com.dzirbel.kotify.ui.components.table.SortSelector
import com.dzirbel.kotify.ui.components.table.Table
import com.dzirbel.kotify.ui.components.trackColumns
import com.dzirbel.kotify.ui.framework.ScrollingPage
import com.dzirbel.kotify.ui.pageStack
import com.dzirbel.kotify.ui.player.Player
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.util.formatDateTime
import kotlinx.coroutines.Dispatchers
import java.time.Instant
import java.util.concurrent.TimeUnit

private object AddedAtColumn : ColumnByString<PlaylistTrack>(name = "Added") {
    // TODO precompute rather than re-parsing each time this is accessed
    private val PlaylistTrack.addedAtTimestamp
        get() = Instant.parse(addedAt.orEmpty()).toEpochMilli()

    override fun toString(item: PlaylistTrack, index: Int): String {
        return formatDateTime(timestamp = item.addedAtTimestamp, includeTime = false)
    }

    override fun compare(first: PlaylistTrack, firstIndex: Int, second: PlaylistTrack, secondIndex: Int): Int {
        return first.addedAtTimestamp.compareTo(second.addedAtTimestamp)
    }
}

@Composable
fun BoxScope.Playlist(page: PlaylistPage) {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val presenter = remember(page) { PlaylistPresenter(page = page, scope = scope) }

    ScrollingPage(scrollState = pageStack.value.currentScrollState, presenter = presenter) { state ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.space4),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.space4),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LoadedImage(url = state.playlist.largestImage.cached?.url)

                Column(verticalArrangement = Arrangement.spacedBy(Dimens.space3)) {
                    Text(state.playlist.name, fontSize = Dimens.fontTitle)

                    state.playlist.description
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { Text(it) }

                    Text(
                        "Created by ${state.playlist.owner.cached.name}; " +
                            "${state.playlist.followersTotal} followers"
                    )

                    val totalDurationMins = remember(state.tracks) {
                        state.tracks?.let { tracks ->
                            TimeUnit.MILLISECONDS.toMinutes(
                                tracks.sumOf { it.track.cached.durationMs.toInt() }.toLong()
                            )
                        }
                    }

                    Text("${state.playlist.totalTracks} songs, ${totalDurationMins ?: "<loading>"} min")

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.space3),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ToggleSaveButton(isSaved = state.isSavedState.value, size = Dimens.iconMedium) {
                            presenter.emitAsync(PlaylistPresenter.Event.ToggleSave(save = it))
                        }

                        PlayButton(context = Player.PlayContext.playlist(state.playlist))
                    }

                    Button(
                        enabled = state.sorts.isNotEmpty() && state.tracks != null && !state.reordering,
                        onClick = {
                            state.tracks?.let { tracks ->
                                presenter.emitAsync(
                                    PlaylistPresenter.Event.Order(sorts = state.sorts, tracks = tracks)
                                )
                            }
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

            InvalidateButton(
                refreshing = state.refreshing,
                updated = state.playlistUpdated,
                updatedFormat = { "Playlist last updated $it" },
                updatedFallback = "Playlist never updated",
                onClick = { presenter.emitAsync(PlaylistPresenter.Event.Load(invalidate = true)) }
            )
        }

        VerticalSpacer(Dimens.space3)

        val tracks = state.tracks
        if (tracks == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            val columns = remember(pageStack) {
                trackColumns(
                    savedTracks = state.savedTracksState.value,
                    onSetTrackSaved = { trackId, saved ->
                        presenter.emitAsync(
                            PlaylistPresenter.Event.ToggleTrackSaved(trackId = trackId, saved = saved)
                        )
                    },
                    trackRatings = state.trackRatings,
                    onRateTrack = { trackId, rating ->
                        presenter.emitAsync(PlaylistPresenter.Event.RateTrack(trackId = trackId, rating = rating))
                    },
                    includeTrackNumber = false,
                    playContextFromIndex = { index ->
                        Player.PlayContext.playlistTrack(playlist = state.playlist, index = index)
                    }
                )
                    .map { column -> column.mapped<PlaylistTrack> { it.track.cached } }
                    .toMutableList()
                    .apply {
                        add(1, IndexColumn())

                        @Suppress("MagicNumber")
                        add(6, AddedAtColumn)
                    }
            }

            // TODO move into playlist header and align right
            SortSelector(
                columns = columns,
                sorts = state.sorts,
                onSetSort = { sorts -> presenter.emitAsync(PlaylistPresenter.Event.SetSorts(sorts = sorts)) }
            )

            Table(
                columns = columns,
                items = tracks,
                sorts = state.sorts,
                onSetSort = { sort ->
                    presenter.emitAsync(PlaylistPresenter.Event.SetSorts(sorts = listOfNotNull(sort)))
                },
            )
        }
    }
}
