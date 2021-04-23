package com.dzirbel.kotify.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
        val albums: Map<String, Album?>?,
        val playlistsUpdated: Long?,
        val playlists: Map<String, Playlist?>?,
        val playlistTracks: Map<String, SpotifyCache.GlobalObjects.PlaylistTracks?>?,
        val tracks: Map<String, Track?>?,
        val tracksUpdated: Long?,

        val refreshingSavedArtists: Boolean = false,
        val refreshingArtists: Set<String> = emptySet()
    )

    sealed class Event {
        object Load : Event()

        object RefreshSavedArtists : Event()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            Event.Load -> {
                val state = State(
                    artists = loadArtists(),
                    artistsUpdated = LibraryCache.artistsUpdated,
                    albumsUpdated = LibraryCache.albumsUpdated,
                    albums = LibraryCache.albums,
                    playlistsUpdated = LibraryCache.playlistsUpdated,
                    playlists = LibraryCache.playlists,
                    playlistTracks = LibraryCache.playlistTracks,
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
        }
    }

    private fun loadArtists(): List<CachedArtist>? {
        val artistAlbums = LibraryCache.artistAlbums
        return LibraryCache.artists?.map { (id, artist) ->
            CachedArtist(
                id = id,
                artist = artist,
                updated = SpotifyCache.lastUpdated(id),
                albums = artistAlbums?.get(id),
                albumsUpdated = SpotifyCache.lastUpdated(SpotifyCache.GlobalObjects.ArtistAlbums.idFor(artistId = id))
            )
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
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space4)) {
            Text("Library State", fontSize = Dimens.fontTitle)

            Text("Artists")
            Column(
                verticalArrangement = Arrangement.spacedBy(Dimens.space2),
                modifier = Modifier.padding(start = Dimens.space3)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space3),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${state.artists?.size} saved artists")

                    InvalidateButton(refreshing = state.refreshingSavedArtists, updated = state.artistsUpdated) {
                        presenter.emitAsync(LibraryStatePresenter.Event.RefreshSavedArtists)
                    }
                }

                val artistsExpanded = remember { mutableStateOf(false) }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space3),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val total = state.artists?.count { it.artist != null }
                    val simplified = state.artists?.count { it.artist is SimplifiedArtist }
                    val full = state.artists?.count { it.artist is FullArtist }
                    val albums = state.artists?.count { it.albums != null }
                    Text("$total in cache | $full full; $simplified simplified | $albums album mappings")

                    SimpleTextButton(
                        enabled = state.artists != null,
                        onClick = { artistsExpanded.value = !artistsExpanded.value }
                    ) {
                        Text(if (artistsExpanded.value) "Collapse" else "Expand")
                    }
                }

                if (artistsExpanded.value && state.artists != null) {
                    Table(columns = artistColumns, items = state.artists.toList())
                }
            }

            Text("Albums")
            Column(
                verticalArrangement = Arrangement.spacedBy(Dimens.space2),
                modifier = Modifier.padding(start = Dimens.space3)
            ) {
                Text("${state.albums?.size} saved albums")

                val simplified = state.albums?.count { it.value is SimplifiedAlbum }
                val full = state.albums?.count { it.value is FullAlbum }
                Text("${state.albums?.count { it.value != null }} in cache | $full full; $simplified simplified")
            }

            Text("Tracks")
            Column(
                verticalArrangement = Arrangement.spacedBy(Dimens.space2),
                modifier = Modifier.padding(start = Dimens.space3)
            ) {
                Text("${state.tracks?.size} saved tracks")

                val simplified = state.tracks?.count { it.value is SimplifiedTrack }
                val full = state.tracks?.count { it.value is FullTrack }
                Text("${state.tracks?.count { it.value != null }} in cache | $full full; $simplified simplified")
            }

            Text("Playlists")
            Column(
                verticalArrangement = Arrangement.spacedBy(Dimens.space2),
                modifier = Modifier.padding(start = Dimens.space3)
            ) {
                Text("${state.playlists?.size} saved playlists")

                val simplified = state.playlists?.count { it.value is SimplifiedPlaylist }
                val full = state.playlists?.count { it.value is FullPlaylist }
                Text("${state.playlists?.count { it.value != null }} in cache | $full full; $simplified simplified")

                Text("${state.playlistTracks?.count { it.value != null }} track mappings in cache")
            }
        }
    }
}
