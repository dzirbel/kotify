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
import com.dzirbel.kotify.network.model.Album
import com.dzirbel.kotify.network.model.Artist
import com.dzirbel.kotify.network.model.FullAlbum
import com.dzirbel.kotify.network.model.FullArtist
import com.dzirbel.kotify.network.model.FullPlaylist
import com.dzirbel.kotify.network.model.FullTrack
import com.dzirbel.kotify.network.model.Playlist
import com.dzirbel.kotify.network.model.SimplifiedAlbum
import com.dzirbel.kotify.network.model.SimplifiedArtist
import com.dzirbel.kotify.network.model.SimplifiedPlaylist
import com.dzirbel.kotify.network.model.SimplifiedTrack
import com.dzirbel.kotify.network.model.Track
import com.dzirbel.kotify.ui.common.Column
import com.dzirbel.kotify.ui.common.ColumnByString
import com.dzirbel.kotify.ui.common.ColumnWidth
import com.dzirbel.kotify.ui.common.InvalidateButton
import com.dzirbel.kotify.ui.common.PageStack
import com.dzirbel.kotify.ui.common.SimpleTextButton
import com.dzirbel.kotify.ui.common.Sort
import com.dzirbel.kotify.ui.common.Table
import com.dzirbel.kotify.ui.common.liveRelativeDateText
import com.dzirbel.kotify.ui.theme.Colors
import com.dzirbel.kotify.ui.theme.Dimens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

private class LibraryStatePresenter(scope: CoroutineScope) :
    Presenter<LibraryStatePresenter.State?, LibraryStatePresenter.Event>(
        scope = scope,
        eventMergeStrategy = EventMergeStrategy.LATEST,
        startingEvents = listOf(Event.Load),
        initialState = null
    ) {

    data class State(
        val artists: List<CachedArtist>?,
        val artistsUpdated: Long?,
        val albumsUpdated: Long?,
        val albums: List<CachedAlbum>?,
        val playlists: List<CachedPlaylist>?,
        val playlistsUpdated: Long?,
        val tracks: Map<String, Track?>?,
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
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            Event.Load -> {
                val state = State(
                    artists = loadArtists(),
                    artistsUpdated = LibraryCache.artistsUpdated,
                    albumsUpdated = LibraryCache.albumsUpdated,
                    albums = loadAlbums(),
                    playlistsUpdated = LibraryCache.playlistsUpdated,
                    playlists = loadPlaylists(),
                    tracks = LibraryCache.tracks,
                    tracksUpdated = LibraryCache.tracksUpdated,
                )

                mutateState { state }
            }

            Event.RefreshSavedArtists -> {
                mutateState { it?.copy(refreshingSavedArtists = true) }

                SpotifyCache.invalidate(SpotifyCache.GlobalObjects.SavedArtists.ID)

                SpotifyCache.Artists.getSavedArtists()

                val artists = loadArtists()
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

                val albums = loadAlbums()
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

                val tracks = LibraryCache.tracks
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

                val playlists = loadPlaylists()
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
                val missingIds = requireNotNull(LibraryCache.artists?.filterValues { it == null })
                SpotifyCache.Artists.getFullArtists(ids = missingIds.keys.toList())

                val artists = loadArtists()
                mutateState { it?.copy(artists = artists) }
            }

            Event.InvalidateArtists -> {
                val ids = requireNotNull(LibraryCache.artists?.filterValues { it != null })
                SpotifyCache.invalidate(ids.keys.toList())

                val artists = loadArtists()
                mutateState { it?.copy(artists = artists) }
            }

            Event.FetchMissingArtistAlbums -> {
                val missingIds = requireNotNull(LibraryCache.artistAlbums?.filterValues { it == null })
                // TODO run in parallel
                missingIds.flatMap { SpotifyCache.Artists.getArtistAlbums(artistId = it.key) }

                val artists = loadArtists()
                mutateState { it?.copy(artists = artists) }
            }

            Event.InvalidateArtistAlbums -> {
                val ids = requireNotNull(LibraryCache.artists?.filterValues { it != null })
                SpotifyCache.invalidate(ids.keys.map { SpotifyCache.GlobalObjects.ArtistAlbums.idFor(artistId = it) })

                val artists = loadArtists()
                mutateState { it?.copy(artists = artists) }
            }
        }
    }

    private fun loadArtists(): List<CachedArtist>? {
        return LibraryCache.artists?.toList()?.let { artists ->
            val artistAlbums = LibraryCache.artistAlbums

            // batch calls for last updates
            val updated = SpotifyCache.lastUpdated(
                artists.map { it.first }
                    .plus(artists.map { SpotifyCache.GlobalObjects.ArtistAlbums.idFor(artistId = it.first) })
            )
            check(updated.size == artists.size * 2)

            artists.mapIndexed { index, (id, artist) ->
                CachedArtist(
                    id = id,
                    artist = artist,
                    updated = updated[index],
                    albums = artistAlbums?.get(id),
                    albumsUpdated = updated[index + artists.size]
                )
            }
        }
    }

    private fun loadAlbums(): List<CachedAlbum>? {
        return LibraryCache.albums?.toList()?.let { albums ->
            // batch calls for last updates
            val updated = SpotifyCache.lastUpdated(albums.map { it.first })

            albums.mapIndexed { index, (id, album) ->
                CachedAlbum(
                    id = id,
                    album = album,
                    updated = updated[index]
                )
            }
        }
    }

    private fun loadPlaylists(): List<CachedPlaylist>? {
        return LibraryCache.playlists?.toList()?.let { playlists ->
            val playlistTracks = LibraryCache.playlistTracks

            // batch calls for last updates
            val updated = SpotifyCache.lastUpdated(
                playlists.map { it.first }
                    .plus(playlists.map { SpotifyCache.GlobalObjects.PlaylistTracks.idFor(playlistId = it.first) })
            )
            check(updated.size == playlists.size * 2)

            playlists.mapIndexed { index, (id, playlist) ->
                CachedPlaylist(
                    id = id,
                    playlist = playlist,
                    updated = updated[index],
                    tracks = playlistTracks?.get(id),
                    tracksUpdated = updated[index + playlists.size]
                )
            }
        }
    }
}

// TODO expose directly from LibraryCache?
private data class CachedArtist(
    val id: String,
    val artist: Artist?,
    val updated: Long?,
    val albums: List<String>?,
    val albumsUpdated: Long?
)

// TODO expose directly from LibraryCache?
private data class CachedAlbum(
    val id: String,
    val album: Album?,
    val updated: Long?
)

// TODO expose directly from LibraryCache?
private data class CachedPlaylist(
    val id: String,
    val playlist: Playlist?,
    val updated: Long?,
    val tracks: SpotifyCache.GlobalObjects.PlaylistTracks?,
    val tracksUpdated: Long?
)

// TODO allow refreshing artist/album
private val artistColumns = listOf(
    object : ColumnByString<CachedArtist>(header = "Name", width = ColumnWidth.Fill()) {
        override fun toString(item: CachedArtist, index: Int): String = item.artist?.name.orEmpty()
    },

    object : ColumnByString<CachedArtist>(header = "ID", width = ColumnWidth.Fill()) {
        override fun toString(item: CachedArtist, index: Int): String = item.id
    },

    object : ColumnByString<CachedArtist>(header = "Type", width = ColumnWidth.Fill()) {
        override fun toString(item: CachedArtist, index: Int): String {
            return item.artist?.let { it::class.java.simpleName }.orEmpty()
        }
    },

    object : Column<CachedArtist>() {
        override val width = ColumnWidth.Fill()

        override fun compare(first: CachedArtist, firstIndex: Int, second: CachedArtist, secondIndex: Int): Int {
            return (first.updated ?: 0).compareTo(second.updated ?: 0)
        }

        @Composable
        override fun header(sort: MutableState<Sort?>) = standardHeader(sort = sort, header = "Artist updated")

        @Composable
        override fun item(item: CachedArtist, index: Int) {
            val text = item.updated?.let { liveRelativeDateText(timestamp = it) }.orEmpty()
            Text(text = text, modifier = Modifier.padding(Dimens.space3))
        }
    },

    object : ColumnByString<CachedArtist>(header = "Albums", width = ColumnWidth.Fill()) {
        override fun compare(first: CachedArtist, firstIndex: Int, second: CachedArtist, secondIndex: Int): Int {
            return (first.albums?.size ?: 0).compareTo(second.albums?.size ?: 0)
        }

        override fun toString(item: CachedArtist, index: Int): String {
            return item.albums?.size?.toString().orEmpty()
        }
    },

    object : Column<CachedArtist>() {
        override val width = ColumnWidth.Fill()

        override fun compare(first: CachedArtist, firstIndex: Int, second: CachedArtist, secondIndex: Int): Int {
            return (first.albumsUpdated ?: 0).compareTo(second.albumsUpdated ?: 0)
        }

        @Composable
        override fun header(sort: MutableState<Sort?>) = standardHeader(sort = sort, header = "Albums updated")

        @Composable
        override fun item(item: CachedArtist, index: Int) {
            val text = item.albumsUpdated?.let { liveRelativeDateText(timestamp = it) }.orEmpty()
            Text(text = text, modifier = Modifier.padding(Dimens.space3))
        }
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
        Table(columns = artistColumns, items = state.artists.toList())
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
                            // TODO
                        }
                    ) {
                        Text("Fetch missing")
                    }

                    DropdownMenuItem(
                        onClick = {
                            // TODO
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
        // TODO albums table
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
            val totalCached = tracks.count { it.value != null }
            val simplified = tracks.count { it.value is SimplifiedTrack }
            val full = tracks.count { it.value is FullTrack }

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
                            // TODO
                        }
                    ) {
                        Text("Fetch missing")
                    }

                    DropdownMenuItem(
                        onClick = {
                            // TODO
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
                            // TODO
                        }
                    ) {
                        Text("Fetch missing")
                    }

                    DropdownMenuItem(
                        onClick = {
                            // TODO
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
                            // TODO
                        }
                    ) {
                        Text("Fetch missing")
                    }

                    DropdownMenuItem(
                        onClick = {
                            // TODO
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
        // TODO playlist table
    }
}
