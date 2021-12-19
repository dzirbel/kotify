package com.dzirbel.kotify.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ContentAlpha
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.cache.LibraryCache
import com.dzirbel.kotify.cache.SpotifyCache
import com.dzirbel.kotify.network.model.FullTrack
import com.dzirbel.kotify.network.model.SimplifiedTrack
import com.dzirbel.kotify.network.model.Track
import com.dzirbel.kotify.ui.components.LinkedText
import com.dzirbel.kotify.ui.components.PageStack
import com.dzirbel.kotify.ui.components.hoverState
import com.dzirbel.kotify.ui.components.table.Column
import com.dzirbel.kotify.ui.components.table.ColumnByNumber
import com.dzirbel.kotify.ui.components.table.ColumnByString
import com.dzirbel.kotify.ui.components.table.ColumnWidth
import com.dzirbel.kotify.ui.components.table.SortOrder
import com.dzirbel.kotify.ui.theme.Colors
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.mutate
import com.dzirbel.kotify.util.compareToNullable
import com.dzirbel.kotify.util.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun trackColumns(
    pageStack: MutableState<PageStack>,
    savedTracks: Set<String>? = LibraryCache.savedTracks,
    includeTrackNumber: Boolean = true,
    includeAlbum: Boolean = true,
    playContextFromIndex: ((Int) -> Player.PlayContext?)?
): List<Column<Track>> {
    return listOfNotNull(
        playContextFromIndex?.let { PlayingColumn(playContextFromIndex = it) },
        TrackNumberColumn.takeIf { includeTrackNumber },
        SavedColumn(savedTracks = savedTracks),
        NameColumn,
        ArtistColumn(pageStack),
        AlbumColumn(pageStack).takeIf { includeAlbum },
        RatingColumn,
        DurationColumn,
        PopularityColumn
    )
}

/**
 * A [Column] which displays the current play state of a [Track] with an icon, and allows playing a [Track] via the
 * [playContext].
 */
class PlayingColumn(
    /**
     * Returns a [Player.PlayContext] to play when the user selects the given [track] at the given [index] in the
     * column.
     */
    private val playContextFromIndex: (Int) -> Player.PlayContext?
) : Column<Track>(name = "Currently playing", sortable = false) {
    override val width = ColumnWidth.Fill()
    override val cellAlignment = Alignment.Center

    @Composable
    override fun header(sortOrder: SortOrder?, onSetSort: (SortOrder?) -> Unit) {
        Box(Modifier)
    }

    @Composable
    override fun item(item: Track, index: Int) {
        val hoverState = remember { mutableStateOf(false) }
        Box(Modifier.hoverState(hoverState).padding(Dimens.space2).size(Dimens.fontBodyDp)) {
            if (Player.currentTrack.value?.id == item.id) {
                CachedIcon(
                    name = "volume-up",
                    size = Dimens.fontBodyDp,
                    contentDescription = "Playing",
                    tint = Colors.current.primary,
                )
            } else {
                if (hoverState.value) {
                    val context = playContextFromIndex(index)
                    IconButton(
                        onClick = { Player.play(context = context) },
                        enabled = context != null,
                    ) {
                        CachedIcon(
                            name = "play-circle-outline",
                            size = Dimens.fontBodyDp,
                            contentDescription = "Play",
                            tint = Colors.current.primary,
                        )
                    }
                }
            }
        }
    }
}

class SavedColumn(savedTracks: Set<String>?) : Column<Track>(name = "Saved", sortable = false) {
    private val savedTracks = mutableStateOf(savedTracks)

    override val width = ColumnWidth.Fill()

    @Composable
    override fun header(sortOrder: SortOrder?, onSetSort: (SortOrder?) -> Unit) {
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

object NameColumn : ColumnByString<Track>(name = "Title") {
    override val width = ColumnWidth.Weighted(weight = 1f)

    override fun toString(item: Track, index: Int) = item.name
}

class ArtistColumn(private val pageStack: MutableState<PageStack>) : Column<Track>(name = "Artist", sortable = true) {
    override val width = ColumnWidth.Weighted(weight = 1f)

    override fun compare(first: Track, firstIndex: Int, second: Track, secondIndex: Int): Int {
        return first.artists.joinToString { it.name }
            .compareTo(second.artists.joinToString { it.name }, ignoreCase = true)
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

class AlbumColumn(private val pageStack: MutableState<PageStack>) : Column<Track>(name = "Album", sortable = true) {
    override val width = ColumnWidth.Weighted(weight = 1f)

    override fun compare(first: Track, firstIndex: Int, second: Track, secondIndex: Int): Int {
        return first.album?.name.orEmpty().compareTo(second.album?.name.orEmpty(), ignoreCase = true)
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

object DurationColumn : ColumnByString<Track>(name = "Duration") {
    override val cellAlignment = Alignment.TopEnd

    override fun toString(item: Track, index: Int) = formatDuration(item.durationMs)

    override fun compare(first: Track, firstIndex: Int, second: Track, secondIndex: Int): Int {
        return first.durationMs.compareTo(second.durationMs)
    }
}

object TrackNumberColumn : ColumnByNumber<Track>(name = "#", sortable = true) {
    override fun toNumber(item: Track, index: Int) = item.trackNumber
}

object PopularityColumn : Column<Track>(name = "Popularity", sortable = true) {
    override val width: ColumnWidth = ColumnWidth.MatchHeader
    override val cellAlignment = Alignment.TopEnd

    private val Track.popularity: Int?
        get() = (this as? FullTrack)?.popularity ?: (this as? SimplifiedTrack)?.popularity

    @Composable
    override fun item(item: Track, index: Int) {
        val popularity = item.popularity ?: 0
        val color = Colors.current.text.copy(alpha = ContentAlpha.disabled)

        Box(
            Modifier
                .padding(Dimens.space3)
                .background(Colors.current.surface2)
                .height(Dimens.fontBodyDp)
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
        return first.popularity.compareToNullable(second.popularity)
    }
}

object RatingColumn : Column<Track>(name = "Rating", sortable = true) {
    override val width: ColumnWidth = ColumnWidth.Fill()
    override val cellAlignment = Alignment.Center

    @Composable
    override fun item(item: Track, index: Int) {
        TrackStarRating(trackId = item.id, modifier = Modifier.padding(horizontal = Dimens.space3))
    }

    override fun compare(first: Track, firstIndex: Int, second: Track, secondIndex: Int): Int {
        val firstRating = first.id?.let { SpotifyCache.Ratings.getRating(trackId = it) }
            ?.let { it.rating.toDouble() / it.maxRating }
        val secondRating = second.id?.let { SpotifyCache.Ratings.getRating(trackId = it) }
            ?.let { it.rating.toDouble() / it.maxRating }

        return firstRating.compareToNullable(secondRating)
    }
}
