package com.dzirbel.kotify.ui.page.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.Playlist
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.components.HorizontalSpacer
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.NameColumn
import com.dzirbel.kotify.ui.components.RatingColumn
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.components.table.ColumnByNumber
import com.dzirbel.kotify.ui.components.table.ColumnByRelativeDateText
import com.dzirbel.kotify.ui.components.table.ColumnByString
import com.dzirbel.kotify.ui.components.table.Sort
import com.dzirbel.kotify.ui.components.table.SortOrder
import com.dzirbel.kotify.ui.components.table.Table
import com.dzirbel.kotify.ui.framework.ScrollingPage
import com.dzirbel.kotify.ui.pageStack
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import kotlinx.coroutines.Dispatchers

private val RATINGS_TABLE_WIDTH = 750.dp

// TODO allow refreshing artist/album
private val artistColumns = listOf(
    object : ColumnByString<Pair<String, Artist?>>(name = "Name") {
        override fun toString(item: Pair<String, Artist?>, index: Int): String = item.second?.name.orEmpty()
    },

    object : ColumnByString<Pair<String, Artist?>>(name = "ID") {
        override fun toString(item: Pair<String, Artist?>, index: Int): String = item.first
    },

    object : ColumnByRelativeDateText<Pair<String, Artist?>>(name = "Artist updated") {
        override fun timestampFor(item: Pair<String, Artist?>, index: Int) = item.second?.updatedTime?.toEpochMilli()
    },

    object : ColumnByNumber<Pair<String, Artist?>>(name = "Albums") {
        override fun toNumber(item: Pair<String, Artist?>, index: Int): Int? {
            return item.second?.let { artist ->
                if (artist.hasAllAlbums) artist.albums.cached.size else null
            }
        }
    },

    object : ColumnByRelativeDateText<Pair<String, Artist?>>(name = "Albums updated") {
        override fun timestampFor(item: Pair<String, Artist?>, index: Int) = item.second?.albumsFetched?.toEpochMilli()
    },
)

// TODO allow refreshing album
private val albumColumns = listOf(
    object : ColumnByString<Pair<String, Album?>>(name = "Name") {
        override fun toString(item: Pair<String, Album?>, index: Int): String = item.second?.name.orEmpty()
    },

    object : ColumnByString<Pair<String, Album?>>(name = "Artists") {
        override fun toString(item: Pair<String, Album?>, index: Int): String {
            return item.second?.artists?.cached?.joinToString { it.name }.orEmpty()
        }
    },

    object : ColumnByString<Pair<String, Album?>>(name = "ID") {
        override fun toString(item: Pair<String, Album?>, index: Int): String = item.first
    },

    object : ColumnByRelativeDateText<Pair<String, Album?>>(name = "Album updated") {
        override fun timestampFor(item: Pair<String, Album?>, index: Int) = item.second?.updatedTime?.toEpochMilli()
    },

    object : ColumnByRelativeDateText<Pair<String, Album?>>(name = "Full updated") {
        override fun timestampFor(item: Pair<String, Album?>, index: Int) = item.second?.fullUpdatedTime?.toEpochMilli()
    },
)

// TODO allow refreshing playlist/tracks
private val playlistColumns = listOf(
    object : ColumnByString<Pair<String, Playlist?>>(name = "Name") {
        override fun toString(item: Pair<String, Playlist?>, index: Int): String = item.second?.name.orEmpty()
    },

    object : ColumnByString<Pair<String, Playlist?>>(name = "ID") {
        override fun toString(item: Pair<String, Playlist?>, index: Int): String = item.first
    },

    object : ColumnByRelativeDateText<Pair<String, Playlist?>>(name = "Playlist updated") {
        override fun timestampFor(item: Pair<String, Playlist?>, index: Int) = item.second?.updatedTime?.toEpochMilli()
    },

    object : ColumnByRelativeDateText<Pair<String, Playlist?>>(name = "Full updated") {
        override fun timestampFor(item: Pair<String, Playlist?>, index: Int): Long? {
            return item.second?.fullUpdatedTime?.toEpochMilli()
        }
    },

    object : ColumnByNumber<Pair<String, Playlist?>>(name = "Tracks") {
        override fun toNumber(item: Pair<String, Playlist?>, index: Int) = item.second?.totalTracks?.toInt()
    },
)

@Composable
fun BoxScope.LibraryState() {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val presenter = remember { LibraryStatePresenter(scope = scope) }

    ScrollingPage(scrollState = pageStack.value.currentScrollState, presenter = presenter) { state ->
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space3)) {
            Text("Library State", fontSize = Dimens.fontTitle)

            Artists(state, presenter)

            Box(Modifier.fillMaxWidth().height(Dimens.divider).background(LocalColors.current.dividerColor))

            Albums(state, presenter)

            Box(Modifier.fillMaxWidth().height(Dimens.divider).background(LocalColors.current.dividerColor))

            Tracks(state, presenter)

            Box(Modifier.fillMaxWidth().height(Dimens.divider).background(LocalColors.current.dividerColor))

            Playlists(state, presenter)

            Box(Modifier.fillMaxWidth().height(Dimens.divider).background(LocalColors.current.dividerColor))

            Ratings(state, presenter)
        }
    }
}

@Composable
private fun Artists(state: LibraryStatePresenter.ViewModel, presenter: LibraryStatePresenter) {
    val artists = state.artists

    if (artists == null) {
        InvalidateButton(
            refreshing = state.refreshingSavedArtists,
            updated = state.artistsUpdated,
            updatedFallback = "Artists never updated",
            iconSize = Dimens.iconTiny
        ) {
            presenter.emitAsync(LibraryStatePresenter.Event.RefreshSavedArtists)
        }

        return
    }

    val artistsExpanded = remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val totalSaved = artists.size
            val totalCached = artists.count { it.second != null }
            val simplified = artists.count { it.second != null && it.second?.fullUpdatedTime == null }
            val full = artists.count { it.second?.fullUpdatedTime != null }
            val albums = artists.count { it.second?.hasAllAlbums == true }

            Text("$totalSaved Saved Artists", modifier = Modifier.padding(end = Dimens.space3))

            InvalidateButton(
                refreshing = state.refreshingSavedArtists,
                updated = state.artistsUpdated,
                iconSize = Dimens.iconTiny
            ) {
                presenter.emitAsync(LibraryStatePresenter.Event.RefreshSavedArtists)
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
                            presenter.emitAsync(LibraryStatePresenter.Event.FetchMissingArtists)
                            inCacheExpanded.value = false
                        }
                    ) {
                        Text("Fetch missing")
                    }

                    DropdownMenuItem(
                        onClick = {
                            presenter.emitAsync(LibraryStatePresenter.Event.InvalidateArtists)
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
                            presenter.emitAsync(LibraryStatePresenter.Event.FetchMissingArtistAlbums)
                            albumMappingsExpanded.value = false
                        }
                    ) {
                        Text("Fetch missing")
                    }

                    DropdownMenuItem(
                        onClick = {
                            presenter.emitAsync(LibraryStatePresenter.Event.InvalidateArtistAlbums)
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
        Table(columns = artistColumns, items = artists.toList())
    }
}

@Composable
private fun Albums(state: LibraryStatePresenter.ViewModel, presenter: LibraryStatePresenter) {
    val albums = state.albums

    if (albums == null) {
        InvalidateButton(
            refreshing = state.refreshingSavedAlbums,
            updated = state.albumsUpdated,
            updatedFallback = "Albums never updated",
            iconSize = Dimens.iconTiny
        ) {
            presenter.emitAsync(LibraryStatePresenter.Event.RefreshSavedAlbums)
        }

        return
    }

    val albumsExpanded = remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val totalSaved = albums.size
            val totalCached = albums.count { it.second != null }
            val simplified = albums.count { it.second != null && it.second?.fullUpdatedTime == null }
            val full = albums.count { it.second?.fullUpdatedTime != null }

            Text("$totalSaved Saved Albums", modifier = Modifier.padding(end = Dimens.space3))

            InvalidateButton(
                refreshing = state.refreshingSavedAlbums,
                updated = state.albumsUpdated,
                iconSize = Dimens.iconTiny
            ) {
                presenter.emitAsync(LibraryStatePresenter.Event.RefreshSavedAlbums)
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
                            presenter.emitAsync(LibraryStatePresenter.Event.FetchMissingAlbums)
                            inCacheExpanded.value = false
                        }
                    ) {
                        Text("Fetch missing")
                    }

                    DropdownMenuItem(
                        onClick = {
                            presenter.emitAsync(LibraryStatePresenter.Event.InvalidateAlbums)
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
        Table(columns = albumColumns, items = albums)
    }
}

@Composable
private fun Tracks(state: LibraryStatePresenter.ViewModel, presenter: LibraryStatePresenter) {
    val tracks = state.tracks

    if (tracks == null) {
        InvalidateButton(
            refreshing = state.refreshingSavedTracks,
            updated = state.tracksUpdated,
            updatedFallback = "Tracks never updated",
            iconSize = Dimens.iconTiny
        ) {
            presenter.emitAsync(LibraryStatePresenter.Event.RefreshSavedTracks)
        }

        return
    }

    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val totalSaved = tracks.size
            val totalCached = tracks.count { it.second != null }
            val simplified = tracks.count { it.second != null && it.second?.fullUpdatedTime == null }
            val full = tracks.count { it.second?.fullUpdatedTime != null }

            Text("$totalSaved Saved Tracks", modifier = Modifier.padding(end = Dimens.space3))

            InvalidateButton(
                refreshing = state.refreshingSavedTracks,
                updated = state.tracksUpdated,
                iconSize = Dimens.iconTiny
            ) {
                presenter.emitAsync(LibraryStatePresenter.Event.RefreshSavedTracks)
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
                            presenter.emitAsync(LibraryStatePresenter.Event.FetchMissingTracks)
                            inCacheExpanded.value = false
                        }
                    ) {
                        Text("Fetch missing")
                    }

                    DropdownMenuItem(
                        onClick = {
                            presenter.emitAsync(LibraryStatePresenter.Event.InvalidateTracks)
                            inCacheExpanded.value = false
                        }
                    ) {
                        Text("Invalidate all")
                    }
                }
            }
        }
    }
}

@Composable
private fun Playlists(state: LibraryStatePresenter.ViewModel, presenter: LibraryStatePresenter) {
    val playlists = state.playlists

    if (playlists == null) {
        InvalidateButton(
            refreshing = state.refreshingSavedPlaylists,
            updated = state.playlistsUpdated,
            updatedFallback = "Playlists never updated",
            iconSize = Dimens.iconTiny
        ) {
            presenter.emitAsync(LibraryStatePresenter.Event.RefreshSavedPlaylists)
        }

        return
    }

    val playlistsExpanded = remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val totalSaved = playlists.size
            val totalCached = playlists.count { it.second != null }
            val simplified = playlists.count { it.second != null && it.second?.fullUpdatedTime == null }
            val full = playlists.count { it.second?.fullUpdatedTime != null }
            val tracks = playlists.count { it.second?.hasAllTracksCached == true }

            Text("$totalSaved Saved Playlists", modifier = Modifier.padding(end = Dimens.space3))

            InvalidateButton(
                refreshing = state.refreshingSavedPlaylists,
                updated = state.playlistsUpdated,
                iconSize = Dimens.iconTiny
            ) {
                presenter.emitAsync(LibraryStatePresenter.Event.RefreshSavedPlaylists)
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
                            presenter.emitAsync(LibraryStatePresenter.Event.FetchMissingPlaylists)
                            inCacheExpanded.value = false
                        }
                    ) {
                        Text("Fetch missing")
                    }

                    DropdownMenuItem(
                        onClick = {
                            presenter.emitAsync(LibraryStatePresenter.Event.InvalidatePlaylists)
                            inCacheExpanded.value = false
                        }
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
                    tint = if (allInCache) Color.Green else Color.Red
                )

                HorizontalSpacer(Dimens.space1)

                Text("$tracks/$totalSaved track mappings")

                DropdownMenu(
                    expanded = trackMappingsExpanded.value,
                    onDismissRequest = { trackMappingsExpanded.value = false }
                ) {
                    DropdownMenuItem(
                        onClick = {
                            presenter.emitAsync(LibraryStatePresenter.Event.FetchMissingPlaylistTracks)
                            trackMappingsExpanded.value = false
                        }
                    ) {
                        Text("Fetch missing")
                    }

                    DropdownMenuItem(
                        onClick = {
                            presenter.emitAsync(LibraryStatePresenter.Event.InvalidatePlaylistTracks)
                            trackMappingsExpanded.value = false
                        }
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
        Table(columns = playlistColumns, items = playlists.toList())
    }
}

@Composable
private fun Ratings(state: LibraryStatePresenter.ViewModel, presenter: LibraryStatePresenter) {
    val ratedTracks = state.ratedTracks

    val ratingsExpanded = remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${ratedTracks.size} Rated Tracks", modifier = Modifier.padding(end = Dimens.space3))

            SimpleTextButton(onClick = { presenter.emitAsync(LibraryStatePresenter.Event.ClearAllRatings) }) {
                Text("Clear all ratings")
            }
        }

        SimpleTextButton(onClick = { ratingsExpanded.value = !ratingsExpanded.value }) {
            Text(if (ratingsExpanded.value) "Collapse" else "Expand")
        }
    }

    if (ratingsExpanded.value) {
        val ratingColumn = remember {
            RatingColumn(
                trackRatings = state.trackRatings,
                onRateTrack = { trackId, rating ->
                    presenter.emitAsync(LibraryStatePresenter.Event.RateTrack(trackId = trackId, rating = rating))
                },
            )
        }

        Table(
            columns = listOf(NameColumn, ratingColumn),
            items = ratedTracks.mapNotNull { it.second },
            modifier = Modifier.widthIn(max = RATINGS_TABLE_WIDTH),
            defaultSortOrder = Sort(ratingColumn, SortOrder.DESCENDING), // sort by rating descending by default
        )
    }
}
