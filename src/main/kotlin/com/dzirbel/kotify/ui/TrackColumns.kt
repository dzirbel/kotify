package com.dzirbel.kotify.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.dzirbel.kotify.cache.LibraryCache
import com.dzirbel.kotify.cache.SpotifyCache
import com.dzirbel.kotify.network.model.FullTrack
import com.dzirbel.kotify.network.model.SimplifiedAlbum
import com.dzirbel.kotify.network.model.SimplifiedTrack
import com.dzirbel.kotify.network.model.Track
import com.dzirbel.kotify.ui.common.Column
import com.dzirbel.kotify.ui.common.ColumnByString
import com.dzirbel.kotify.ui.common.ColumnWidth
import com.dzirbel.kotify.ui.common.LinkedText
import com.dzirbel.kotify.ui.common.PageStack
import com.dzirbel.kotify.ui.common.Sort
import com.dzirbel.kotify.ui.theme.Colors
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.mutate
import com.dzirbel.kotify.util.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun trackColumns(
    pageStack: MutableState<PageStack>,
    savedTracks: Set<String>? = LibraryCache.savedTracks,
    includeTrackNumber: Boolean = true,
    includeAlbum: Boolean = true
): List<Column<Track>> {
    return listOfNotNull(
        PlayingColumn,
        TrackNumberColumn.takeIf { includeTrackNumber },
        SavedColumn(savedTracks = savedTracks),
        NameColumn,
        ArtistColumn(pageStack),
        AlbumColumn(pageStack).takeIf { includeAlbum },
        DurationColumn,
        PopularityColumn
    )
}

object PlayingColumn : Column<Track>() {
    override val width = ColumnWidth.Fill()

    override val verticalAlignment = Alignment.CenterVertically

    override fun compare(first: Track, firstIndex: Int, second: Track, secondIndex: Int): Int {
        error("cannot compare by playing state")
    }

    @Composable
    override fun header(sort: MutableState<Sort?>) {
        Box(Modifier)
    }

    @Composable
    override fun item(item: Track, index: Int) {
        if (Player.currentTrack.value?.id == item.id) {
            val fontSizeDp = with(LocalDensity.current) { Dimens.fontBody.toDp() }
            CachedIcon(
                name = "volume-up",
                size = fontSizeDp,
                contentDescription = "Playing",
                modifier = Modifier.padding(horizontal = Dimens.space2),
                tint = Colors.current.primary
            )
        } else {
            Box(Modifier)
        }
    }
}

class SavedColumn(savedTracks: Set<String>?) : Column<Track>() {
    private val savedTracks = mutableStateOf(savedTracks)

    override val width = ColumnWidth.Fill()
    override val verticalAlignment = Alignment.Top

    override fun compare(first: Track, firstIndex: Int, second: Track, secondIndex: Int): Int {
        error("cannot compare by saved state")
    }

    @Composable
    override fun header(sort: MutableState<Sort?>) {
        Box(Modifier)
    }

    @Composable
    override fun item(item: Track, index: Int) {
        item.id?.let { trackId ->
            val scope = rememberCoroutineScope { Dispatchers.IO }
            ToggleSaveButton(
                modifier = Modifier.padding(Dimens.space2),
                isSaved = savedTracks.value?.contains(item.id)
            ) { save ->
                scope.launch {
                    if (save) {
                        SpotifyCache.Tracks.saveTrack(id = trackId)
                        savedTracks.mutate { this?.plus(trackId) }
                    } else {
                        SpotifyCache.Tracks.unsaveTrack(id = trackId)
                        savedTracks.mutate { this?.minus(trackId) }
                    }
                }
            }
        } ?: Box(Modifier)
    }
}

object NameColumn : ColumnByString<Track>(header = "Title", width = ColumnWidth.Weighted(weight = 1f)) {
    override fun toString(item: Track, index: Int) = item.name
}

class ArtistColumn(private val pageStack: MutableState<PageStack>) : Column<Track>() {
    override val width = ColumnWidth.Weighted(weight = 1f)

    override fun compare(first: Track, firstIndex: Int, second: Track, secondIndex: Int): Int {
        return first.artists.joinToString { it.name }
            .compareTo(second.artists.joinToString { it.name }, ignoreCase = true)
    }

    @Composable
    override fun header(sort: MutableState<Sort?>) {
        standardHeader(sort = sort, header = "Artist")
    }

    @Composable
    override fun item(item: Track, index: Int) {
        LinkedText(
            modifier = Modifier.padding(Dimens.space3),
            key = item,
            onClickLink = { artistId ->
                pageStack.mutate { to(ArtistPage(artistId = artistId)) }
            }
        ) {
            list(item.artists) { artist ->
                link(text = artist.name, link = artist.id)
            }
        }
    }
}

class AlbumColumn(private val pageStack: MutableState<PageStack>) : Column<Track>() {
    override val width = ColumnWidth.Weighted(weight = 1f)

    private val Track.album: SimplifiedAlbum?
        get() = (this as? FullTrack)?.album ?: (this as? SimplifiedTrack)?.album

    override fun compare(first: Track, firstIndex: Int, second: Track, secondIndex: Int): Int {
        return first.album?.name.orEmpty().compareTo(second.album?.name.orEmpty(), ignoreCase = true)
    }

    @Composable
    override fun header(sort: MutableState<Sort?>) {
        standardHeader(sort = sort, header = "Album")
    }

    @Composable
    override fun item(item: Track, index: Int) {
        LinkedText(
            modifier = Modifier.padding(Dimens.space3),
            key = item,
            onClickLink = { albumId ->
                pageStack.mutate { to(AlbumPage(albumId = albumId)) }
            }
        ) {
            item.album?.let { album ->
                link(text = album.name, link = album.id)
            }
        }
    }
}

object DurationColumn : ColumnByString<Track>(
    header = "Duration",
    width = ColumnWidth.Fill(),
    horizontalAlignment = Alignment.End
) {
    override fun toString(item: Track, index: Int) = formatDuration(item.durationMs)

    override fun compare(first: Track, firstIndex: Int, second: Track, secondIndex: Int): Int {
        return first.durationMs.compareTo(second.durationMs)
    }
}

object TrackNumberColumn : ColumnByString<Track>(header = "#", width = ColumnWidth.Fill()) {
    override fun toString(item: Track, index: Int) = item.trackNumber.toString()

    override fun compare(first: Track, firstIndex: Int, second: Track, secondIndex: Int): Int {
        return first.trackNumber.compareTo(second.trackNumber)
    }
}

object PopularityColumn : Column<Track>() {
    override val width: ColumnWidth = ColumnWidth.MatchHeader
    override val horizontalAlignment = Alignment.End

    private val Track.popularity: Int?
        get() = (this as? FullTrack)?.popularity ?: (this as? SimplifiedTrack)?.popularity

    @Composable
    override fun header(sort: MutableState<Sort?>) {
        standardHeader(sort = sort, header = "Popularity")
    }

    @Composable
    override fun item(item: Track, index: Int) {
        val popularity = item.popularity ?: 0
        val height = with(LocalDensity.current) { Dimens.fontBody.toDp() }
        val color = Colors.current.text.copy(alpha = ContentAlpha.disabled)

        Box(
            Modifier
                .padding(Dimens.space3)
                .background(Colors.current.surface2)
                .height(height)
                .fillMaxWidth()
                .border(width = Dimens.divider, color = color)
        ) {
            Box(
                @Suppress("MagicNumber")
                Modifier
                    .background(color)
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = popularity / 100f)
            )
        }
    }

    override fun compare(first: Track, firstIndex: Int, second: Track, secondIndex: Int): Int {
        val firstPopularity = first.popularity
        val secondPopularity = second.popularity

        return when {
            firstPopularity != null && secondPopularity != null -> firstPopularity.compareTo(secondPopularity)
            firstPopularity != null -> -1 // second is null -> first before second
            secondPopularity != null -> 1 // first is null -> second before first
            else -> 0
        }
    }
}
