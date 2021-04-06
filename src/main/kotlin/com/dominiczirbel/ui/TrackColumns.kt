package com.dominiczirbel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.ContentAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.dominiczirbel.network.model.FullTrack
import com.dominiczirbel.network.model.SimplifiedTrack
import com.dominiczirbel.network.model.Track
import com.dominiczirbel.ui.common.Column
import com.dominiczirbel.ui.common.ColumnByString
import com.dominiczirbel.ui.common.ColumnWidth
import com.dominiczirbel.ui.common.LinkedText
import com.dominiczirbel.ui.common.PageStack
import com.dominiczirbel.ui.common.Sort
import com.dominiczirbel.ui.theme.Colors
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.ui.util.mutate
import com.dominiczirbel.util.formatDuration

fun trackColumns(
    pageStack: MutableState<PageStack>,
    includeTrackNumber: Boolean = true,
    includeAlbum: Boolean = true
): List<Column<Track>> {
    return listOfNotNull(
        TrackNumberColumn.takeIf { includeTrackNumber },
        NameColumn,
        ArtistColumn(pageStack),
        AlbumColumn.takeIf { includeAlbum },
        DurationColumn,
        PopularityColumn
    )
}

object NameColumn : ColumnByString<Track>(header = "Title", width = ColumnWidth.Weighted(weight = 1f)) {
    override fun toString(item: Track, index: Int) = item.name
}

class ArtistColumn(private val pageStack: MutableState<PageStack>) : Column<Track>() {
    override val width = ColumnWidth.Weighted(weight = 1f)

    override fun compare(first: Track, firstIndex: Int, second: Track, secondIndex: Int): Int {
        return first.artists.joinToString().compareTo(second.artists.joinToString())
    }

    @Composable
    override fun header(sort: MutableState<Sort?>) {
        standardHeader(sort = sort, header = "Artist")
    }

    @Composable
    override fun item(item: Track, index: Int) {
        LinkedText(
            modifier = Modifier.padding(Dimens.space3),
            onClickLink = { artistId ->
                pageStack.mutate { to(ArtistPage(artistId = artistId)) }
            }
        ) {
            item.artists.forEachIndexed { index, artist ->
                artist.id?.let {
                    link(text = artist.name, link = artist.id)
                    if (index != item.artists.lastIndex) {
                        text(", ")
                    }
                }
            }
        }
    }
}

object AlbumColumn : ColumnByString<Track>(header = "Album", width = ColumnWidth.Weighted(weight = 1f)) {
    override fun toString(item: Track, index: Int): String {
        return ((item as? FullTrack)?.album ?: (item as? SimplifiedTrack)?.album)?.name.orEmpty()
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
    override val width: ColumnWidth = ColumnWidth.Fill()
    override val horizontalAlignment = Alignment.End

    // TODO make the column width match the header rather than hardcoding
    private val WIDTH = 70.dp

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
                .width(WIDTH)
                .border(width = 1.dp, color = color)
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
