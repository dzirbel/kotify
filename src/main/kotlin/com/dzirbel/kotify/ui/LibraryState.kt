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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.AlbumRepository
import com.dzirbel.kotify.db.model.AlbumTable
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.ArtistRepository
import com.dzirbel.kotify.db.model.ArtistTable
import com.dzirbel.kotify.db.model.Playlist
import com.dzirbel.kotify.db.model.PlaylistRepository
import com.dzirbel.kotify.db.model.PlaylistTrackTable
import com.dzirbel.kotify.db.model.SavedAlbumRepository
import com.dzirbel.kotify.db.model.SavedArtistRepository
import com.dzirbel.kotify.db.model.SavedPlaylistRepository
import com.dzirbel.kotify.db.model.SavedTrackRepository
import com.dzirbel.kotify.db.model.Track
import com.dzirbel.kotify.db.model.TrackRatingRepository
import com.dzirbel.kotify.db.model.TrackRepository
import com.dzirbel.kotify.repository.Rating
import com.dzirbel.kotify.ui.components.HorizontalSpacer
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.PageStack
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.components.table.ColumnByNumber
import com.dzirbel.kotify.ui.components.table.ColumnByRelativeDateText
import com.dzirbel.kotify.ui.components.table.ColumnByString
import com.dzirbel.kotify.ui.components.table.Sort
import com.dzirbel.kotify.ui.components.table.SortOrder
import com.dzirbel.kotify.ui.components.table.Table
import com.dzirbel.kotify.ui.theme.Colors
import com.dzirbel.kotify.ui.theme.Dimens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.update

private val RATINGS_TABLE_WIDTH = 750.dp

private class LibraryStatePresenter(scope: CoroutineScope) :
    Presenter<LibraryStatePresenter.ViewModel?, LibraryStatePresenter.Event>(
        scope = scope,
        eventMergeStrategy = EventMergeStrategy.LATEST,
        startingEvents = listOf(Event.Load),
        initialState = null
    ) {

    data class ViewModel(
        // pair artistId, artist? in case we have the ID cached but not artist
        val artists: List<Pair<String, Artist?>>?,
        val artistsUpdated: Long?,

        // pair albumId, album? in case we have the ID cached but not album
        val albums: List<Pair<String, Album?>>?,
        val albumsUpdated: Long?,

        val playlists: List<Pair<String, Playlist?>>?,
        val playlistsUpdated: Long?,

        val tracks: List<Pair<String, Track?>>?,
        val tracksUpdated: Long?,

        val ratedTracks: List<Pair<String, Track?>>,
        val trackRatings: Map<String, State<Rating?>>,

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

        object ClearAllRatings : Event()
        data class RateTrack(val trackId: String, val rating: Rating?) : Event()

        object FetchMissingPlaylists : Event()
        object InvalidatePlaylists : Event()
        object FetchMissingPlaylistTracks : Event()
        object InvalidatePlaylistTracks : Event()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            Event.Load -> {
                val savedArtistIds = SavedArtistRepository.getLibraryCached()?.toList()
                val savedArtists = savedArtistIds?.let { ArtistRepository.getCached(ids = it) }

                val savedAlbumIds = SavedAlbumRepository.getLibraryCached()?.toList()
                val savedAlbums = savedAlbumIds?.let { AlbumRepository.getCached(ids = it) }

                val savedTracksIds = SavedTrackRepository.getLibraryCached()?.toList()
                val savedTracks = savedTracksIds?.let { TrackRepository.getCached(ids = it) }

                val savedPlaylistIds = SavedPlaylistRepository.getLibraryCached()?.toList()
                val savedPlaylists = savedPlaylistIds?.let { PlaylistRepository.getCached(ids = it) }
                KotifyDatabase.transaction {
                    savedPlaylists?.onEach { it?.tracks?.loadToCache() }
                }

                val ratedTrackIds = TrackRatingRepository.ratedEntities().toList()
                val ratedTracks = TrackRepository.get(ids = ratedTrackIds)
                val trackRatings = ratedTrackIds.associateWith { TrackRatingRepository.ratingState(it) }

                val state = ViewModel(
                    artists = savedArtistIds?.zip(savedArtists!!),
                    artistsUpdated = SavedArtistRepository.libraryUpdated()?.toEpochMilli(),
                    albums = savedAlbumIds?.zip(savedAlbums!!),
                    albumsUpdated = SavedAlbumRepository.libraryUpdated()?.toEpochMilli(),
                    playlists = savedPlaylistIds?.zip(savedPlaylists!!),
                    playlistsUpdated = SavedPlaylistRepository.libraryUpdated()?.toEpochMilli(),
                    tracks = savedTracksIds?.zip(savedTracks!!),
                    tracksUpdated = SavedTrackRepository.libraryUpdated()?.toEpochMilli(),
                    ratedTracks = ratedTrackIds.zip(ratedTracks),
                    trackRatings = trackRatings,
                )

                mutateState { state }
            }

            Event.RefreshSavedArtists -> {
                mutateState { it?.copy(refreshingSavedArtists = true) }

                SavedArtistRepository.invalidateLibrary()

                val artistIds = SavedArtistRepository.getLibrary().toList()
                val artists = ArtistRepository.getCached(ids = artistIds)
                val artistsUpdated = SavedArtistRepository.libraryUpdated()?.toEpochMilli()

                mutateState {
                    it?.copy(
                        artists = artistIds.zip(artists),
                        artistsUpdated = artistsUpdated,
                        refreshingSavedArtists = false,
                    )
                }
            }

            Event.RefreshSavedAlbums -> {
                mutateState { it?.copy(refreshingSavedAlbums = true) }

                SavedAlbumRepository.invalidateLibrary()

                val albumIds = SavedAlbumRepository.getLibrary().toList()
                val albums = AlbumRepository.getCached(ids = albumIds)
                val albumsUpdated = SavedAlbumRepository.libraryUpdated()?.toEpochMilli()

                mutateState {
                    it?.copy(
                        albums = albumIds.zip(albums),
                        albumsUpdated = albumsUpdated,
                        refreshingSavedAlbums = false,
                    )
                }
            }

            Event.RefreshSavedTracks -> {
                mutateState { it?.copy(refreshingSavedTracks = true) }

                SavedTrackRepository.invalidateLibrary()

                val trackIds = SavedTrackRepository.getLibrary().toList()
                val tracks = TrackRepository.getCached(ids = trackIds)
                val tracksUpdated = SavedTrackRepository.libraryUpdated()?.toEpochMilli()

                mutateState {
                    it?.copy(
                        tracks = trackIds.zip(tracks),
                        tracksUpdated = tracksUpdated,
                        refreshingSavedTracks = false
                    )
                }
            }

            Event.RefreshSavedPlaylists -> {
                mutateState { it?.copy(refreshingSavedPlaylists = true) }

                SavedPlaylistRepository.invalidateLibrary()

                val playlistIds = SavedPlaylistRepository.getLibrary().toList()
                val playlists = PlaylistRepository.getCached(ids = playlistIds)
                KotifyDatabase.transaction {
                    playlists.onEach { it?.tracks?.loadToCache() }
                }
                val playlistsUpdated = SavedPlaylistRepository.libraryUpdated()?.toEpochMilli()

                mutateState {
                    it?.copy(
                        playlists = playlistIds.zip(playlists),
                        playlistsUpdated = playlistsUpdated,
                        refreshingSavedPlaylists = false
                    )
                }
            }

            Event.FetchMissingArtists -> {
                val artistIds = requireNotNull(SavedArtistRepository.getLibraryCached()).toList()
                val artists = ArtistRepository.getFull(ids = artistIds)

                mutateState { it?.copy(artists = artistIds.zip(artists)) }
            }

            Event.InvalidateArtists -> {
                val artistIds = requireNotNull(SavedArtistRepository.getLibraryCached()).toList()
                ArtistRepository.invalidate(ids = artistIds)
                val artists = ArtistRepository.getCached(ids = artistIds)

                mutateState { it?.copy(artists = artistIds.zip(artists)) }
            }

            Event.FetchMissingArtistAlbums -> {
                val artistIds = requireNotNull(SavedArtistRepository.getLibraryCached()).toList()
                val missingIds = KotifyDatabase.transaction {
                    Artist.find { ArtistTable.albumsFetched eq null }
                        .map { it.id.value }
                }
                    .filter { artistIds.contains(it) }

                missingIds
                    .asFlow()
                    .flatMapMerge { id ->
                        flow<Unit> { Artist.getAllAlbums(artistId = id) }
                    }
                    .collect()

                val artists = ArtistRepository.getCached(ids = artistIds)
                mutateState { it?.copy(artists = artistIds.zip(artists)) }
            }

            Event.InvalidateArtistAlbums -> {
                KotifyDatabase.transaction {
                    AlbumTable.AlbumArtistTable.deleteAll()

                    ArtistTable.update(where = { Op.TRUE }) {
                        it[albumsFetched] = null
                    }
                }

                val artistIds = requireNotNull(SavedArtistRepository.getLibraryCached()).toList()
                val artists = ArtistRepository.getCached(ids = artistIds)
                mutateState { it?.copy(artists = artistIds.zip(artists)) }
            }

            Event.FetchMissingAlbums -> {
                val albumIds = requireNotNull(SavedAlbumRepository.getLibraryCached()).toList()
                val albums = AlbumRepository.getFull(ids = albumIds)

                mutateState { it?.copy(albums = albumIds.zip(albums)) }
            }

            Event.InvalidateAlbums -> {
                val albumIds = requireNotNull(SavedAlbumRepository.getLibraryCached()).toList()
                AlbumRepository.invalidate(ids = albumIds)
                val albums = AlbumRepository.getCached(ids = albumIds)

                mutateState { it?.copy(albums = albumIds.zip(albums)) }
            }

            Event.FetchMissingTracks -> {
                val trackIds = requireNotNull(SavedTrackRepository.getLibraryCached()).toList()
                val tracks = TrackRepository.getFull(ids = trackIds)

                mutateState { it?.copy(tracks = trackIds.zip(tracks)) }
            }

            Event.InvalidateTracks -> {
                val trackIds = requireNotNull(SavedTrackRepository.getLibraryCached()).toList()
                TrackRepository.invalidate(ids = trackIds)
                val tracks = TrackRepository.getCached(ids = trackIds)

                mutateState { it?.copy(tracks = trackIds.zip(tracks)) }
            }

            Event.ClearAllRatings -> {
                TrackRatingRepository.clearAllRatings()
                mutateState { it?.copy(ratedTracks = emptyList()) }
            }

            is Event.RateTrack -> TrackRatingRepository.rate(id = event.trackId, rating = event.rating)

            Event.FetchMissingPlaylists -> {
                val playlistIds = requireNotNull(SavedPlaylistRepository.getLibraryCached()).toList()
                val playlists = PlaylistRepository.getFull(ids = playlistIds)
                KotifyDatabase.transaction {
                    playlists.onEach { it?.tracks?.loadToCache() }
                }

                mutateState { it?.copy(playlists = playlistIds.zip(playlists)) }
            }

            Event.InvalidatePlaylists -> {
                val playlistIds = requireNotNull(SavedPlaylistRepository.getLibraryCached()).toList()
                PlaylistRepository.invalidate(ids = playlistIds)
                val playlists = PlaylistRepository.getCached(ids = playlistIds)
                KotifyDatabase.transaction {
                    playlists.onEach { it?.tracks?.loadToCache() }
                }

                mutateState { it?.copy(playlists = playlistIds.zip(playlists)) }
            }

            Event.FetchMissingPlaylistTracks -> {
                val playlistIds = requireNotNull(SavedPlaylistRepository.getLibraryCached()).toList()
                val playlists = PlaylistRepository.get(ids = playlistIds)
                KotifyDatabase.transaction {
                    playlists.onEach { it?.tracks?.loadToCache() }
                }

                // TODO also fetch tracks for playlists not in the database at all
                val missingTracks = KotifyDatabase.transaction {
                    playlists.filter { it?.hasAllTracks == false }
                }

                missingTracks
                    .asFlow()
                    .flatMapMerge { playlist ->
                        flow<Unit> { playlist?.getAllTracks() }
                    }
                    .collect()

                mutateState { it?.copy(playlists = playlistIds.zip(playlists)) }
            }

            Event.InvalidatePlaylistTracks -> {
                KotifyDatabase.transaction { PlaylistTrackTable.deleteAll() }

                val playlistIds = requireNotNull(SavedPlaylistRepository.getLibraryCached()).toList()
                val playlists = PlaylistRepository.getCached(ids = playlistIds)
                mutateState { it?.copy(playlists = playlistIds.zip(playlists)) }
            }
        }
    }
}

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
fun BoxScope.LibraryState(pageStack: MutableState<PageStack>) {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val presenter = remember { LibraryStatePresenter(scope = scope) }

    ScrollingPage(scrollState = pageStack.value.currentScrollState, presenter = presenter) { state ->
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space3)) {
            Text("Library State", fontSize = Dimens.fontTitle)

            Artists(state, presenter)

            Box(Modifier.fillMaxWidth().height(Dimens.divider).background(Colors.current.dividerColor))

            Albums(state, presenter)

            Box(Modifier.fillMaxWidth().height(Dimens.divider).background(Colors.current.dividerColor))

            Tracks(state, presenter)

            Box(Modifier.fillMaxWidth().height(Dimens.divider).background(Colors.current.dividerColor))

            Playlists(state, presenter)

            Box(Modifier.fillMaxWidth().height(Dimens.divider).background(Colors.current.dividerColor))

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
