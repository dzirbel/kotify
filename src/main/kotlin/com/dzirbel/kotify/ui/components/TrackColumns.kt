package com.dzirbel.kotify.ui.components

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
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.db.model.Track
import com.dzirbel.kotify.network.model.SpotifyTrack
import com.dzirbel.kotify.repository.Rating
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.components.adapter.SortOrder
import com.dzirbel.kotify.ui.components.adapter.SortableProperty
import com.dzirbel.kotify.ui.components.adapter.compare
import com.dzirbel.kotify.ui.components.adapter.compareNullable
import com.dzirbel.kotify.ui.components.star.StarRating
import com.dzirbel.kotify.ui.components.table.Column
import com.dzirbel.kotify.ui.components.table.ColumnByLinkedText
import com.dzirbel.kotify.ui.components.table.ColumnByNumber
import com.dzirbel.kotify.ui.components.table.ColumnByString
import com.dzirbel.kotify.ui.components.table.ColumnWidth
import com.dzirbel.kotify.ui.page.album.AlbumPage
import com.dzirbel.kotify.ui.page.artist.ArtistPage
import com.dzirbel.kotify.ui.pageStack
import com.dzirbel.kotify.ui.player.Player
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.theme.surfaceBackground
import com.dzirbel.kotify.ui.util.mutate
import com.dzirbel.kotify.util.formatDuration

fun trackColumns(
    savedTracks: Set<String>?,
    onSetTrackSaved: (trackId: String, saved: Boolean) -> Unit,
    trackRatings: Map<String, State<Rating?>>?,
    onRateTrack: (trackId: String, rating: Rating?) -> Unit,
    includeTrackNumber: Boolean = true,
    includeAlbum: Boolean = true,
    playContextFromIndex: ((Int) -> Player.PlayContext?)?,
): List<Column<Track>> {
    return listOfNotNull(
        playContextFromIndex?.let { PlayingColumn(playContextFromIndex = it) },
        TrackNumberColumn.takeIf { includeTrackNumber },
        SavedColumn(savedTracks = savedTracks, onSetTrackSaved = onSetTrackSaved),
        NameColumn,
        ArtistColumn,
        AlbumColumn.takeIf { includeAlbum },
        RatingColumn(trackRatings = trackRatings, onRateTrack = onRateTrack),
        DurationColumn,
        PopularityColumn,
    )
}

/**
 * A [Column] which displays the current play state of a [SpotifyTrack] with an icon, and allows playing a
 * [SpotifyTrack] via the [playContextFromIndex].
 */
class PlayingColumn(
    /**
     * Returns a [Player.PlayContext] to play when the user selects the track at the given index in the column.
     */
    private val playContextFromIndex: (index: Int) -> Player.PlayContext?,
) : Column<Track>(name = "Currently playing") {
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
            if (Player.currentTrack.value?.id == item.id.value) {
                CachedIcon(
                    name = "volume-up",
                    size = Dimens.fontBodyDp,
                    contentDescription = "Playing",
                    tint = LocalColors.current.primary,
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
                            tint = LocalColors.current.primary,
                        )
                    }
                }
            }
        }
    }
}

class SavedColumn(
    private val savedTracks: Set<String>?,
    private val onSetTrackSaved: (trackId: String, saved: Boolean) -> Unit,
) : Column<Track>(name = "Saved") {
    override val width = ColumnWidth.Fill()

    @Composable
    override fun header(sortOrder: SortOrder?, onSetSort: (SortOrder?) -> Unit) {
        Box(Modifier)
    }

    @Composable
    override fun item(item: Track, index: Int) {
        val trackId = item.id.value

        ToggleSaveButton(
            modifier = Modifier.padding(Dimens.space2),
            isSaved = savedTracks?.contains(trackId),
        ) { save ->
            onSetTrackSaved(trackId, save)
        }
    }
}

object NameColumn : ColumnByString<Track>(name = "Title") {
    override val width = ColumnWidth.Weighted(weight = 1f)

    override fun toString(item: Track, index: Int) = item.name
}

object ArtistColumn : ColumnByLinkedText<Track>(name = "Artist") {
    override val width = ColumnWidth.Weighted(weight = 1f)

    override fun links(item: Track, index: Int): List<Link> {
        return item.artists.cached.map { artist ->
            Link(text = artist.name, link = artist.id.value)
        }
    }

    override fun onClickLink(link: String) {
        pageStack.mutate { to(ArtistPage(artistId = link)) }
    }
}

object AlbumColumn : ColumnByLinkedText<Track>(name = "Album") {
    override val width = ColumnWidth.Weighted(weight = 1f)

    override fun links(item: Track, index: Int): List<Link> {
        return listOf(
            Link(text = item.name, link = item.id.value)
        )
    }

    override fun onClickLink(link: String) {
        pageStack.mutate { to(AlbumPage(albumId = link)) }
    }
}

object DurationColumn : ColumnByString<Track>(name = "Duration") {
    override val cellAlignment = Alignment.TopEnd

    override val sortableProperty = object : SortableProperty<Track>(sortTitle = name) {
        override fun compare(sortOrder: SortOrder, first: IndexedValue<Track>, second: IndexedValue<Track>): Int {
            return sortOrder.compare(first.value.durationMs, second.value.durationMs)
        }
    }

    override fun toString(item: Track, index: Int) = formatDuration(item.durationMs.toLong())
}

object TrackNumberColumn : ColumnByNumber<Track>(name = "#") {
    override fun toNumber(item: Track, index: Int) = item.trackNumber.toInt()
}

object PopularityColumn : Column<Track>(name = "Popularity") {
    override val width: ColumnWidth = ColumnWidth.MatchHeader
    override val cellAlignment = Alignment.TopEnd

    override val sortableProperty = object : SortableProperty<Track>(sortTitle = name) {
        override fun compare(sortOrder: SortOrder, first: IndexedValue<Track>, second: IndexedValue<Track>): Int {
            return sortOrder.compareNullable(first.value.popularity, second.value.popularity)
        }
    }

    @Composable
    override fun item(item: Track, index: Int) {
        val popularity = item.popularity?.toInt() ?: 0
        val color = LocalColors.current.text.copy(alpha = ContentAlpha.disabled)

        LocalColors.current.withSurface {
            Box(
                Modifier
                    .padding(Dimens.space3)
                    .surfaceBackground()
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
    }
}

class RatingColumn(
    private val trackRatings: Map<String, State<Rating?>>?,
    private val onRateTrack: (trackId: String, rating: Rating?) -> Unit,
) : Column<Track>(name = "Rating") {
    override val width: ColumnWidth = ColumnWidth.Fill()
    override val cellAlignment = Alignment.Center

    override val sortableProperty = object : SortableProperty<Track>(sortTitle = name) {
        override fun compare(sortOrder: SortOrder, first: IndexedValue<Track>, second: IndexedValue<Track>): Int {
            val firstRating = trackRatings?.get(first.value.id.value)?.value?.ratingPercent
            val secondRating = trackRatings?.get(second.value.id.value)?.value?.ratingPercent

            return sortOrder.compareNullable(firstRating, secondRating)
        }
    }

    @Composable
    override fun item(item: Track, index: Int) {
        val trackId = item.id.value
        StarRating(
            rating = trackRatings?.get(trackId)?.value,
            onRate = { rating -> onRateTrack(trackId, rating) },
        )
    }
}
