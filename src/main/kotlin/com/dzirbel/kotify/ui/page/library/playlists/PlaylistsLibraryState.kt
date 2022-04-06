package com.dzirbel.kotify.ui.page.library.playlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.dzirbel.kotify.db.model.Playlist
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.components.HorizontalSpacer
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.components.table.Column
import com.dzirbel.kotify.ui.components.table.ColumnByNumber
import com.dzirbel.kotify.ui.components.table.ColumnByString
import com.dzirbel.kotify.ui.components.table.Table
import com.dzirbel.kotify.ui.page.library.InvalidateButtonColumn
import com.dzirbel.kotify.ui.theme.Dimens

private fun playlistColumns(
    presenter: PlaylistsLibraryStatePresenter,
    playlists: Map<String, Playlist>,
    syncingPlaylists: Set<String>,
    syncingPlaylistTracks: Set<String>,
): List<Column<String>> {
    return listOf(
        object : ColumnByString<String> {
            override val title = "Name"

            override fun toString(item: String): String = playlists[item]?.name.orEmpty()
        },

        object : ColumnByString<String> {
            override val title = "ID"

            override fun toString(item: String): String = item
        },

        object : InvalidateButtonColumn<String> {
            override val title = "Sync playlist"

            override fun timestampFor(item: String) = playlists[item]?.updatedTime?.toEpochMilli()

            override fun isRefreshing(item: String) = syncingPlaylists.contains(item)

            override fun onInvalidate(item: String) {
                presenter.emitAsync(PlaylistsLibraryStatePresenter.Event.RefreshPlaylist(playlistId = item))
            }
        },

        object : ColumnByNumber<String> {
            override val title = "Tracks"

            override fun toNumber(item: String) = playlists[item]?.totalTracks
        },

        object : InvalidateButtonColumn<String> {
            override val title = "Sync tracks"

            override fun timestampFor(item: String): Long? {
                return playlists[item]?.tracksFetched?.toEpochMilli()
            }

            override fun isRefreshing(item: String) = syncingPlaylistTracks.contains(item)

            override fun onInvalidate(item: String) {
                presenter.emitAsync(PlaylistsLibraryStatePresenter.Event.RefreshPlaylistTracks(playlistId = item))
            }
        },
    )
}

@Composable
fun PlaylistsLibraryState(presenter: PlaylistsLibraryStatePresenter) {
    val state = presenter.state().stateOrThrow

    if (!state.savedPlaylistIds.hasElements) {
        InvalidateButton(
            refreshing = state.syncingSavedPlaylists,
            updated = state.playlistsUpdated,
            updatedFallback = "Playlists never synced",
        ) {
            presenter.emitAsync(PlaylistsLibraryStatePresenter.Event.Load(fromCache = false))
        }

        return
    }

    val playlistsExpanded = remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val totalSaved = state.savedPlaylistIds.size
            val totalCached = state.savedPlaylistIds.count { state.playlists[it] != null }
            val full = state.savedPlaylistIds.count { state.playlists[it]?.fullUpdatedTime != null }
            val simplified = totalCached - full
            val tracks = state.savedPlaylistIds.count { state.playlists[it]?.hasAllTracks == true }

            Text("$totalSaved Saved Playlists", modifier = Modifier.padding(end = Dimens.space3))

            InvalidateButton(
                refreshing = state.syncingSavedPlaylists,
                updated = state.playlistsUpdated,
            ) {
                presenter.emitAsync(PlaylistsLibraryStatePresenter.Event.Load(fromCache = false))
            }

            val inCacheExpanded = remember { mutableStateOf(false) }
            SimpleTextButton(onClick = { inCacheExpanded.value = true }) {
                val allInCache = full == totalSaved
                CachedIcon(
                    name = if (allInCache) "check-circle" else "cancel",
                    size = Dimens.iconSmall,
                    tint = if (allInCache) Color.Green else Color.Red,
                )

                HorizontalSpacer(Dimens.space1)

                Text(
                    "$totalCached/$totalSaved in cache" +
                        simplified.takeIf { it > 0 }?.let { " ($it simplified)" }.orEmpty(),
                )

                DropdownMenu(
                    expanded = inCacheExpanded.value,
                    onDismissRequest = { inCacheExpanded.value = false },
                ) {
                    DropdownMenuItem(
                        enabled = full < totalSaved,
                        onClick = {
                            presenter.emitAsync(PlaylistsLibraryStatePresenter.Event.FetchMissingPlaylists)
                            inCacheExpanded.value = false
                        },
                    ) {
                        Text("Fetch missing")
                    }

                    DropdownMenuItem(
                        onClick = {
                            presenter.emitAsync(PlaylistsLibraryStatePresenter.Event.InvalidatePlaylists)
                            inCacheExpanded.value = false
                        },
                    ) {
                        Text("Invalidate all")
                    }
                }
            }

            val trackMappingsExpanded = remember { mutableStateOf(false) }
            SimpleTextButton(onClick = { trackMappingsExpanded.value = true }) {
                val allInCache = tracks == totalSaved
                CachedIcon(
                    name = if (allInCache) "check-circle" else "cancel",
                    size = Dimens.iconSmall,
                    tint = if (allInCache) Color.Green else Color.Red,
                )

                HorizontalSpacer(Dimens.space1)

                Text("$tracks/$totalSaved track mappings")

                DropdownMenu(
                    expanded = trackMappingsExpanded.value,
                    onDismissRequest = { trackMappingsExpanded.value = false },
                ) {
                    DropdownMenuItem(
                        onClick = {
                            presenter.emitAsync(PlaylistsLibraryStatePresenter.Event.FetchMissingPlaylistTracks)
                            trackMappingsExpanded.value = false
                        },
                    ) {
                        Text("Fetch missing")
                    }

                    DropdownMenuItem(
                        onClick = {
                            presenter.emitAsync(PlaylistsLibraryStatePresenter.Event.InvalidatePlaylistTracks)
                            trackMappingsExpanded.value = false
                        },
                    ) {
                        Text("Invalidate all")
                    }
                }
            }
        }

        SimpleTextButton(onClick = { playlistsExpanded.value = !playlistsExpanded.value }) {
            Text(if (playlistsExpanded.value) "Collapse" else "Expand")
        }
    }

    if (playlistsExpanded.value) {
        Table(
            columns = playlistColumns(
                presenter = presenter,
                playlists = state.playlists,
                syncingPlaylists = state.syncingPlaylists,
                syncingPlaylistTracks = state.syncingPlaylistTracks,
            ),
            items = state.savedPlaylistIds,
            onSetSort = {
                presenter.emitAsync(PlaylistsLibraryStatePresenter.Event.SetSort(sorts = listOfNotNull(it)))
            },
        )
    }
}
