package com.dzirbel.kotify.ui.page.library.albums

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.components.HorizontalSpacer
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.components.table.Column
import com.dzirbel.kotify.ui.components.table.ColumnByString
import com.dzirbel.kotify.ui.components.table.Table
import com.dzirbel.kotify.ui.page.library.InvalidateButtonColumn
import com.dzirbel.kotify.ui.theme.Dimens
import kotlinx.coroutines.Dispatchers

private fun albumColumns(
    presenter: AlbumsLibraryStatePresenter,
    albums: Map<String, Album>,
    syncingAlbums: Set<String>,
): List<Column<String>> {
    return listOf(
        object : ColumnByString<String>(name = "Name") {
            override fun toString(item: String, index: Int): String = albums[item]?.name.orEmpty()
        },

        object : ColumnByString<String>(name = "Artists") {
            override fun toString(item: String, index: Int): String {
                return albums[item]?.artists?.cached?.joinToString { it.name }.orEmpty()
            }
        },

        object : ColumnByString<String>(name = "ID") {
            override fun toString(item: String, index: Int): String = item
        },

        object : InvalidateButtonColumn<String>(name = "Sync album") {
            override fun timestampFor(item: String, index: Int) = albums[item]?.updatedTime?.toEpochMilli()

            override fun isRefreshing(item: String, index: Int) = syncingAlbums.contains(item)

            override fun onInvalidate(item: String, index: Int) {
                presenter.emitAsync(AlbumsLibraryStatePresenter.Event.RefreshAlbum(albumId = item))
            }
        },
    )
}

@Composable
fun AlbumsLibraryState() {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val presenter = remember { AlbumsLibraryStatePresenter(scope) }

    presenter.state().stateOrThrow?.let { state ->
        val savedAlbumIds = state.savedAlbumIds

        if (savedAlbumIds == null) {
            InvalidateButton(
                refreshing = state.refreshingSavedAlbums,
                updated = state.albumsUpdated,
                updatedFallback = "Albums never synced",
            ) {
                presenter.emitAsync(AlbumsLibraryStatePresenter.Event.RefreshSavedAlbums)
            }

            return
        }

        val albumsExpanded = remember { mutableStateOf(false) }

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val totalSaved = savedAlbumIds.size
                val totalCached = savedAlbumIds.count { state.albums[it] != null }
                val full = savedAlbumIds.count { state.albums[it]?.fullUpdatedTime != null }
                val simplified = totalCached - full

                Text("$totalSaved Saved Albums", modifier = Modifier.padding(end = Dimens.space3))

                InvalidateButton(
                    refreshing = state.refreshingSavedAlbums,
                    updated = state.albumsUpdated,
                ) {
                    presenter.emitAsync(AlbumsLibraryStatePresenter.Event.RefreshSavedAlbums)
                }

                val inCacheExpanded = remember { mutableStateOf(false) }
                SimpleTextButton(onClick = { inCacheExpanded.value = true }) {
                    val allInCache = full == totalSaved
                    CachedIcon(
                        name = if (allInCache) "check-circle" else "cancel",
                        size = Dimens.iconSmall,
                        tint = if (allInCache) Color.Green else Color.Red
                    )

                    HorizontalSpacer(Dimens.space1)

                    Text(
                        "$totalCached/$totalSaved in cache" +
                            simplified.takeIf { it > 0 }?.let { " ($it simplified)" }.orEmpty()
                    )

                    DropdownMenu(
                        expanded = inCacheExpanded.value,
                        onDismissRequest = { inCacheExpanded.value = false }
                    ) {
                        DropdownMenuItem(
                            enabled = full < totalSaved,
                            onClick = {
                                presenter.emitAsync(AlbumsLibraryStatePresenter.Event.FetchMissingAlbums)
                                inCacheExpanded.value = false
                            }
                        ) {
                            Text("Fetch missing")
                        }

                        DropdownMenuItem(
                            onClick = {
                                presenter.emitAsync(AlbumsLibraryStatePresenter.Event.InvalidateAlbums)
                                inCacheExpanded.value = false
                            }
                        ) {
                            Text("Invalidate all")
                        }
                    }
                }
            }

            SimpleTextButton(onClick = { albumsExpanded.value = !albumsExpanded.value }) {
                Text(if (albumsExpanded.value) "Collapse" else "Expand")
            }
        }

        if (albumsExpanded.value) {
            Table(
                columns = albumColumns(
                    presenter = presenter,
                    albums = state.albums,
                    syncingAlbums = state.syncingAlbums,
                ),
                items = state.savedAlbumIds,
                onSetSort = {
                    presenter.emitAsync(AlbumsLibraryStatePresenter.Event.SetSort(sorts = listOfNotNull(it)))
                }
            )
        }
    }
}
