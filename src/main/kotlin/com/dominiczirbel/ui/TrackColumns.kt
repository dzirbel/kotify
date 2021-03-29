package com.dominiczirbel.ui

import androidx.compose.ui.Alignment
import com.dominiczirbel.network.model.FullTrack
import com.dominiczirbel.network.model.SimplifiedTrack
import com.dominiczirbel.network.model.Track
import com.dominiczirbel.ui.common.ColumnByString
import com.dominiczirbel.ui.common.ColumnWidth
import com.dominiczirbel.util.formatDuration

val StandardTrackColumns = listOf(
    TrackNumberColumn,
    NameColumn,
    ArtistColumn,
    AlbumColumn,
    DurationColumn,
    PopularityColumn
)

object NameColumn : ColumnByString<Track>(header = "Title", width = ColumnWidth.Weighted(weight = 1f)) {
    override fun toString(item: Track, index: Int) = item.name
}

object ArtistColumn : ColumnByString<Track>(header = "Artist", width = ColumnWidth.Weighted(weight = 1f)) {
    override fun toString(item: Track, index: Int) = item.artists.joinToString { artist -> artist.name }
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
}

object TrackNumberColumn : ColumnByString<Track>(header = "#", width = ColumnWidth.Fill()) {
    override fun toString(item: Track, index: Int) = item.trackNumber.toString()
}

object PopularityColumn : ColumnByString<Track>(
    header = "Popularity",
    width = ColumnWidth.Fill(),
    horizontalAlignment = Alignment.End
) {
    override fun toString(item: Track, index: Int): String {
        return ((item as? FullTrack)?.popularity ?: (item as? SimplifiedTrack)?.popularity)?.toString().orEmpty()
    }
}
