package com.dzirbel.kotify.ui.page.library.artists

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
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.components.HorizontalSpacer
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.components.table.Column
import com.dzirbel.kotify.ui.components.table.ColumnByLinkedText
import com.dzirbel.kotify.ui.components.table.ColumnByNumber
import com.dzirbel.kotify.ui.components.table.ColumnByString
import com.dzirbel.kotify.ui.components.table.Table
import com.dzirbel.kotify.ui.framework.rememberPresenter
import com.dzirbel.kotify.ui.page.artist.ArtistPage
import com.dzirbel.kotify.ui.page.library.InvalidateButtonColumn
import com.dzirbel.kotify.ui.pageStack
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.mutate

private fun artistColumns(
    presenter: ArtistsLibraryStatePresenter,
    artists: Map<String, Artist>,
    syncingArtists: Set<String>,
    syncingArtistAlbums: Set<String>,
): List<Column<String>> {
    return listOf(
        object : ColumnByLinkedText<String>(name = "Name") {
            override fun links(item: String, index: Int): List<Link> {
                return listOfNotNull(
                    artists[item]?.let { Link(text = it.name, link = it.id.value) }
                )
            }

            override fun onClickLink(link: String) {
                pageStack.mutate { to(ArtistPage(artistId = link)) }
            }
        },

        object : ColumnByString<String>(name = "ID") {
            override fun toString(item: String, index: Int): String = item
        },

        object : InvalidateButtonColumn<String>(name = "Sync artist") {
            override fun timestampFor(item: String, index: Int) = artists[item]?.updatedTime?.toEpochMilli()

            override fun isRefreshing(item: String, index: Int) = syncingArtists.contains(item)

            override fun onInvalidate(item: String, index: Int) {
                presenter.emitAsync(ArtistsLibraryStatePresenter.Event.RefreshArtist(artistId = item))
            }
        },

        object : ColumnByNumber<String>(name = "Albums") {
            override fun toNumber(item: String, index: Int): Int? {
                return artists[item]?.let { artist ->
                    if (artist.hasAllAlbums) artist.albums.cached.size else null
                }
            }
        },

        object : InvalidateButtonColumn<String>(name = "Sync albums") {
            override fun timestampFor(item: String, index: Int) = artists[item]?.albumsFetched?.toEpochMilli()

            override fun isRefreshing(item: String, index: Int) = syncingArtistAlbums.contains(item)

            override fun onInvalidate(item: String, index: Int) {
                presenter.emitAsync(ArtistsLibraryStatePresenter.Event.RefreshArtistAlbums(artistId = item))
            }
        },
    )
}

@Composable
fun ArtistsLibraryState() {
    val presenter = rememberPresenter { scope -> ArtistsLibraryStatePresenter(scope) }

    presenter.state().stateOrThrow?.let { state ->
        val savedArtistIds = state.savedArtistIds

        if (savedArtistIds == null) {
            InvalidateButton(
                refreshing = state.syncingSavedArtists,
                updated = state.artistsUpdated,
                updatedFallback = "Artists never synced",
            ) {
                presenter.emitAsync(ArtistsLibraryStatePresenter.Event.Load(fromCache = false))
            }

            return
        }

        val artistsExpanded = remember { mutableStateOf(false) }

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val totalSaved = savedArtistIds.size
                val totalCached = savedArtistIds.count { state.artists[it] != null }
                val full = savedArtistIds.count { state.artists[it]?.fullUpdatedTime != null }
                val simplified = totalCached - full
                val albums = savedArtistIds.count { state.artists[it]?.albumsFetched != null }

                Text("$totalSaved Saved Artists", modifier = Modifier.padding(end = Dimens.space3))

                InvalidateButton(
                    refreshing = state.syncingSavedArtists,
                    updated = state.artistsUpdated,
                ) {
                    presenter.emitAsync(ArtistsLibraryStatePresenter.Event.Load(fromCache = false))
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
                                presenter.emitAsync(ArtistsLibraryStatePresenter.Event.FetchMissingArtists)
                                inCacheExpanded.value = false
                            }
                        ) {
                            Text("Fetch missing")
                        }

                        DropdownMenuItem(
                            onClick = {
                                presenter.emitAsync(ArtistsLibraryStatePresenter.Event.InvalidateArtists)
                                inCacheExpanded.value = false
                            }
                        ) {
                            Text("Invalidate all")
                        }
                    }
                }

                val albumMappingsExpanded = remember { mutableStateOf(false) }
                SimpleTextButton(onClick = { albumMappingsExpanded.value = true }) {
                    val allInCache = albums == totalSaved
                    CachedIcon(
                        name = if (allInCache) "check-circle" else "cancel",
                        size = Dimens.iconSmall,
                        tint = if (allInCache) Color.Green else Color.Red
                    )

                    HorizontalSpacer(Dimens.space1)

                    Text("$albums/$totalSaved album mappings")

                    DropdownMenu(
                        expanded = albumMappingsExpanded.value,
                        onDismissRequest = { albumMappingsExpanded.value = false }
                    ) {
                        DropdownMenuItem(
                            onClick = {
                                presenter.emitAsync(ArtistsLibraryStatePresenter.Event.FetchMissingArtistAlbums)
                                albumMappingsExpanded.value = false
                            }
                        ) {
                            Text("Fetch missing")
                        }

                        DropdownMenuItem(
                            onClick = {
                                presenter.emitAsync(ArtistsLibraryStatePresenter.Event.InvalidateArtistAlbums)
                                albumMappingsExpanded.value = false
                            }
                        ) {
                            Text("Invalidate all")
                        }
                    }
                }
            }

            SimpleTextButton(onClick = { artistsExpanded.value = !artistsExpanded.value }) {
                Text(if (artistsExpanded.value) "Collapse" else "Expand")
            }
        }

        if (artistsExpanded.value) {
            Table(
                columns = artistColumns(
                    presenter = presenter,
                    artists = state.artists,
                    syncingArtists = state.syncingArtists,
                    syncingArtistAlbums = state.syncingArtistsAlbums,
                ),
                items = state.savedArtistIds,
                onSetSort = {
                    presenter.emitAsync(ArtistsLibraryStatePresenter.Event.SetSort(sorts = listOfNotNull(it)))
                },
            )
        }
    }
}
