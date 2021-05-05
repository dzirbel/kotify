package com.dzirbel.kotify.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.cache.LibraryCache
import com.dzirbel.kotify.cache.SpotifyCache
import com.dzirbel.kotify.network.model.FullAlbum
import com.dzirbel.kotify.network.model.FullArtist
import com.dzirbel.kotify.network.model.FullPlaylist
import com.dzirbel.kotify.network.model.FullTrack
import com.dzirbel.kotify.network.model.SimplifiedAlbum
import com.dzirbel.kotify.network.model.SimplifiedArtist
import com.dzirbel.kotify.network.model.SimplifiedPlaylist
import com.dzirbel.kotify.network.model.SimplifiedTrack
import com.dzirbel.kotify.ui.common.ColumnByNumber
import com.dzirbel.kotify.ui.common.ColumnByRelativeDateText
import com.dzirbel.kotify.ui.common.ColumnByString
import com.dzirbel.kotify.ui.common.ColumnWidth
import com.dzirbel.kotify.ui.common.InvalidateButton
import com.dzirbel.kotify.ui.common.PageStack
import com.dzirbel.kotify.ui.common.SimpleTextButton
import com.dzirbel.kotify.ui.common.Table
import com.dzirbel.kotify.ui.theme.Colors
import com.dzirbel.kotify.ui.theme.Dimens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow

private class LibraryStatePresenter(scope: CoroutineScope) :
    Presenter<LibraryStatePresenter.State?, LibraryStatePresenter.Event>(
        scope = scope,
        eventMergeStrategy = EventMergeStrategy.LATEST,
        startingEvents = listOf(Event.Load),
        initialState = null
    ) {

    data class State(
        val artists: List<LibraryCache.CachedArtist>?,
        val artistsUpdated: Long?,

        val albums: List<LibraryCache.CachedAlbum>?,
        val albumsUpdated: Long?,

        val playlists: List<LibraryCache.CachedPlaylist>?,
        val playlistsUpdated: Long?,

        val tracks: List<LibraryCache.CachedTrack>?,
        val tracksUpdated: Long?,

        val refreshingSavedArtists: Boolean = false,
        val refreshingArtists: Set<String> = emptySet(),

        val refreshingSavedAlbums: Boolean = false,

        val refreshingSavedTracks: Boolean = false,

        val refreshingSavedPlaylists: Boolean = false
    )

    sealed class Event {
        object Load : Event()

        object RefreshSavedArtists : Event()
        object RefreshSavedAlbums : Event()
        object RefreshSavedTracks : Event()
        object RefreshSavedPlaylists : Event()

        object FetchMissingArtists : Event()
        object InvalidateArtists : Event()
        object FetchMissingArtistAlbums : Event()
        object InvalidateArtistAlbums : Event()

        object FetchMissingAlbums : Event()
        object InvalidateAlbums : Event()

        object FetchMissingTracks : Event()
        object InvalidateTracks : Event()

        object FetchMissingPlaylists : Event()
        object InvalidatePlaylists : Event()
        object FetchMissingPlaylistTracks : Event()
        object InvalidatePlaylistTracks : Event()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            Event.Load -> {
                val state = State(
                    artists = LibraryCache.cachedArtists,
                    artistsUpdated = LibraryCache.artistsUpdated,
                    albumsUpdated = LibraryCache.albumsUpdated,
                    albums = LibraryCache.cachedAlbums,
                    playlistsUpdated = LibraryCache.playlistsUpdated,
                    playlists = LibraryCache.cachedPlaylists,
                    tracks = LibraryCache.cachedTracks,
                    tracksUpdated = LibraryCache.tracksUpdated,
                )

                mutateState { state }
            }

            Event.RefreshSavedArtists -> {
                mutateState { it?.copy(refreshingSavedArtists = true) }

                SpotifyCache.invalidate(SpotifyCache.GlobalObjects.SavedArtists.ID)

                SpotifyCache.Artists.getSavedArtists()

                val artists = LibraryCache.cachedArtists
                val artistsUpdated = LibraryCache.artistsUpdated
                mutateState {
                    it?.copy(
                        artists = artists,
                        artistsUpdated = artistsUpdated,
                        refreshingSavedArtists = false
                    )
                }
            }

            Event.RefreshSavedAlbums -> {
                mutateState { it?.copy(refreshingSavedAlbums = true) }

                SpotifyCache.invalidate(SpotifyCache.GlobalObjects.SavedAlbums.ID)

                SpotifyCache.Albums.getSavedAlbums()

                val albums = LibraryCache.cachedAlbums
                val albumsUpdated = LibraryCache.albumsUpdated
                mutateState {
                    it?.copy(
                        albums = albums,
                        albumsUpdated = albumsUpdated,
                        refreshingSavedAlbums = false
                    )
                }
            }

            Event.RefreshSavedTracks -> {
                mutateState { it?.copy(refreshingSavedTracks = true) }

                SpotifyCache.invalidate(SpotifyCache.GlobalObjects.SavedTracks.ID)

                SpotifyCache.Tracks.getSavedTracks()

                val tracks = LibraryCache.cachedTracks
                val tracksUpdated = LibraryCache.tracksUpdated
                mutateState {
                    it?.copy(
                        tracks = tracks,
                        tracksUpdated = tracksUpdated,
                        refreshingSavedTracks = false
                    )
                }
            }

            Event.RefreshSavedPlaylists -> {
                mutateState { it?.copy(refreshingSavedPlaylists = true) }

                SpotifyCache.invalidate(SpotifyCache.GlobalObjects.SavedPlaylists.ID)

                SpotifyCache.Playlists.getSavedPlaylists()

                val playlists = LibraryCache.cachedPlaylists
                val playlistsUpdated = LibraryCache.playlistsUpdated
                mutateState {
                    it?.copy(
                        playlists = playlists,
                        playlistsUpdated = playlistsUpdated,
                        refreshingSavedPlaylists = false
                    )
                }
            }

            Event.FetchMissingArtists -> {
                val missingIds = requireNotNull(LibraryCache.artists?.filterValues { it !is FullArtist })
                SpotifyCache.Artists.getFullArtists(ids = missingIds.keys.toList())

                val artists = LibraryCache.cachedArtists
                mutateState { it?.copy(artists = artists) }
            }

            Event.InvalidateArtists -> {
                val ids = requireNotNull(LibraryCache.artists?.filterValues { it != null })
                SpotifyCache.invalidate(ids.keys.toList())

                val artists = LibraryCache.cachedArtists
                mutateState { it?.copy(artists = artists) }
            }

            Event.FetchMissingArtistAlbums -> {
                val missingIds = requireNotNull(LibraryCache.artistAlbums?.filterValues { it == null })
                missingIds.keys
                    .asFlow()
                    .flatMapMerge { id ->
                        flow<Unit> { SpotifyCache.Artists.getArtistAlbums(artistId = id) }
                    }
                    .collect()

                val artists = LibraryCache.cachedArtists
                mutateState { it?.copy(artists = artists) }
            }

            Event.InvalidateArtistAlbums -> {
                val ids = requireNotNull(LibraryCache.artists?.filterValues { it != null })
                SpotifyCache.invalidate(ids.keys.map { SpotifyCache.GlobalObjects.ArtistAlbums.idFor(artistId = it) })

                val artists = LibraryCache.cachedArtists
                mutateState { it?.copy(artists = artists) }
            }

            Event.FetchMissingAlbums -> {
                val missingIds = requireNotNull(LibraryCache.albums?.filterValues { it !is FullAlbum })
                SpotifyCache.Albums.getAlbums(ids = missingIds.keys.toList())

                val albums = LibraryCache.cachedAlbums
                mutateState { it?.copy(albums = albums) }
            }

            Event.InvalidateAlbums -> {
                val ids = requireNotNull(LibraryCache.albums?.filterValues { it != null })
                SpotifyCache.invalidate(ids.keys.toList())

                val albums = LibraryCache.cachedAlbums
                mutateState { it?.copy(albums = albums) }
            }

            Event.FetchMissingTracks -> {
                val missingIds = requireNotNull(LibraryCache.tracks?.filterValues { it !is FullTrack })
                SpotifyCache.Tracks.getFullTracks(ids = missingIds.keys.toList())

                val tracks = LibraryCache.cachedTracks
                mutateState { it?.copy(tracks = tracks) }
            }

            Event.InvalidateTracks -> {
                val ids = requireNotNull(LibraryCache.tracks?.filterValues { it != null })
                SpotifyCache.invalidate(ids.keys.toList())

                val tracks = LibraryCache.cachedTracks
                mutateState { it?.copy(tracks = tracks) }
            }

            Event.FetchMissingPlaylists -> {
                val missingIds = requireNotNull(LibraryCache.playlists?.filterValues { it !is FullPlaylist })
                missingIds.keys
                    .asFlow()
                    .flatMapMerge { id ->
                        flow<Unit> { SpotifyCache.Playlists.getFullPlaylist(id = id) }
                    }
                    .collect()

                val playlists = LibraryCache.cachedPlaylists
                mutateState { it?.copy(playlists = playlists) }
            }

            Event.InvalidatePlaylists -> {
                val ids = requireNotNull(LibraryCache.playlists?.filterValues { it != null })
                SpotifyCache.invalidate(ids.keys.toList())

                val playlists = LibraryCache.cachedPlaylists
                mutateState { it?.copy(playlists = playlists) }
            }

            Event.FetchMissingPlaylistTracks -> {
                val missingIds = requireNotNull(LibraryCache.playlistTracks?.filterValues { it == null })

                missingIds.keys
                    .asFlow()
                    .flatMapMerge { id ->
                        flow<Unit> { SpotifyCache.Playlists.getPlaylistTracks(playlistId = id) }
                    }
                    .collect()

                val playlists = LibraryCache.cachedPlaylists
                mutateState { it?.copy(playlists = playlists) }
            }

            Event.InvalidatePlaylistTracks -> {
                val ids = requireNotNull(LibraryCache.playlistTracks?.filterValues { it != null })
                SpotifyCache.invalidate(ids.keys.toList())

                val playlists = LibraryCache.cachedPlaylists
                mutateState { it?.copy(playlists = playlists) }
            }
        }
    }
}

// TODO allow refreshing artist/album
private val artistColumns = listOf(
    object : ColumnByString<LibraryCache.CachedArtist>(header = "Name", width = ColumnWidth.Fill()) {
        override fun toString(item: LibraryCache.CachedArtist, index: Int): String = item.artist?.name.orEmpty()
    },

    object : ColumnByString<LibraryCache.CachedArtist>(header = "ID", width = ColumnWidth.Fill()) {
        override fun toString(item: LibraryCache.CachedArtist, index: Int): String = item.id
    },

    object : ColumnByString<LibraryCache.CachedArtist>(header = "Type", width = ColumnWidth.Fill()) {
        override fun toString(item: LibraryCache.CachedArtist, index: Int): String {
            return item.artist?.let { it::class.java.simpleName }.orEmpty()
        }
    },

    object : ColumnByRelativeDateText<LibraryCache.CachedArtist>(
        header = "Artist updated",
        width = ColumnWidth.Fill()
    ) {
        override fun relativeDate(item: LibraryCache.CachedArtist, index: Int) = item.updated
    },

    object : ColumnByNumber<LibraryCache.CachedArtist>(header = "Albums", width = ColumnWidth.Fill()) {
        override fun toNumber(item: LibraryCache.CachedArtist, index: Int) = item.albums?.size
    },

    object : ColumnByRelativeDateText<LibraryCache.CachedArtist>(
        header = "Albums updated",
        width = ColumnWidth.Fill()
    ) {
        override fun relativeDate(item: LibraryCache.CachedArtist, index: Int) = item.albumsUpdated
    },
)

// TODO allow refreshing album
private val albumColumns = listOf(
    object : ColumnByString<LibraryCache.CachedAlbum>(header = "Name", width = ColumnWidth.Fill()) {
        override fun toString(item: LibraryCache.CachedAlbum, index: Int): String = item.album?.name.orEmpty()
    },

    object : ColumnByString<LibraryCache.CachedAlbum>(header = "Artists", width = ColumnWidth.Fill()) {
        override fun toString(item: LibraryCache.CachedAlbum, index: Int): String {
            return item.album?.artists?.joinToString { it.name }.orEmpty()
        }
    },

    object : ColumnByString<LibraryCache.CachedAlbum>(header = "ID", width = ColumnWidth.Fill()) {
        override fun toString(item: LibraryCache.CachedAlbum, index: Int): String = item.id
    },

    object : ColumnByString<LibraryCache.CachedAlbum>(header = "Type", width = ColumnWidth.Fill()) {
        override fun toString(item: LibraryCache.CachedAlbum, index: Int): String {
            return item.album?.let { it::class.java.simpleName }.orEmpty()
        }
    },

    object : ColumnByRelativeDateText<LibraryCache.CachedAlbum>(header = "Album updated", width = ColumnWidth.Fill()) {
        override fun relativeDate(item: LibraryCache.CachedAlbum, index: Int) = item.updated
    },
)

// TODO allow refreshing playlist/tracks
private val playlistColumns = listOf(
    object : ColumnByString<LibraryCache.CachedPlaylist>(header = "Name", width = ColumnWidth.Fill()) {
        override fun toString(item: LibraryCache.CachedPlaylist, index: Int): String = item.playlist?.name.orEmpty()
    },

    object : ColumnByString<LibraryCache.CachedPlaylist>(header = "ID", width = ColumnWidth.Fill()) {
        override fun toString(item: LibraryCache.CachedPlaylist, index: Int): String = item.id
    },

    object : ColumnByString<LibraryCache.CachedPlaylist>(header = "Type", width = ColumnWidth.Fill()) {
        override fun toString(item: LibraryCache.CachedPlaylist, index: Int): String {
            return item.playlist?.let { it::class.java.simpleName }.orEmpty()
        }
    },

    object : ColumnByRelativeDateText<LibraryCache.CachedPlaylist>(
        header = "Playlist updated",
        width = ColumnWidth.Fill()
    ) {
        override fun relativeDate(item: LibraryCache.CachedPlaylist, index: Int) = item.updated
    },

    object : ColumnByNumber<LibraryCache.CachedPlaylist>(header = "Tracks", width = ColumnWidth.Fill()) {
        override fun toNumber(item: LibraryCache.CachedPlaylist, index: Int) = item.tracks?.trackIds?.size
    },

    object : ColumnByRelativeDateText<LibraryCache.CachedPlaylist>(
        header = "Tracks updated",
        width = ColumnWidth.Fill()
    ) {
        override fun relativeDate(item: LibraryCache.CachedPlaylist, index: Int) = item.tracksUpdated
    },
)

@Composable
fun BoxScope.LibraryState(pageStack: MutableState<PageStack>) {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val presenter = remember { LibraryStatePresenter(scope = scope) }

    ScrollingPage(scrollState = pageStack.value.currentScrollState, state = { presenter.state() }) { state ->
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space3)) {
            Text("Library State", fontSize = Dimens.fontTitle)

            Artists(state, presenter)

            Box(Modifier.fillMaxWidth().height(Dimens.divider).background(Colors.current.dividerColor))

            Albums(state, presenter)

            Box(Modifier.fillMaxWidth().height(Dimens.divider).background(Colors.current.dividerColor))

            Tracks(state, presenter)

            Box(Modifier.fillMaxWidth().height(Dimens.divider).background(Colors.current.dividerColor))

            Playlists(state, presenter)
        }
    }
}

@Composable
private fun Artists(state: LibraryStatePresenter.State, presenter: LibraryStatePresenter) {
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
            val totalCached = artists.count { it.artist != null }
            val simplified = artists.count { it.artist is SimplifiedArtist }
            val full = artists.count { it.artist is FullArtist }
            val albums = artists.count { it.albums != null }

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
private fun Albums(state: LibraryStatePresenter.State, presenter: LibraryStatePresenter) {
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
            val totalCached = albums.count { it.album != null }
            val simplified = albums.count { it.album is SimplifiedAlbum }
            val full = albums.count { it.album is FullAlbum }

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
        Table(columns = albumColumns, items = albums.toList())
    }
}

@Composable
private fun Tracks(state: LibraryStatePresenter.State, presenter: LibraryStatePresenter) {
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
            val totalCached = tracks.count { it.track != null }
            val simplified = tracks.count { it.track is SimplifiedTrack }
            val full = tracks.count { it.track is FullTrack }

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
private fun Playlists(state: LibraryStatePresenter.State, presenter: LibraryStatePresenter) {
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
            val totalCached = playlists.count { it.playlist != null }
            val simplified = playlists.count { it.playlist is SimplifiedPlaylist }
            val full = playlists.count { it.playlist is FullPlaylist }
            val tracks = playlists.count { it.tracks != null }

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
