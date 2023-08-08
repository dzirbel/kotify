package com.dzirbel.kotify.ui.properties

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.dzirbel.kotify.repository.playlist.PlaylistTrackViewModel
import com.dzirbel.kotify.repository.rating.TrackRatingRepository
import com.dzirbel.kotify.repository.track.SavedTrackRepository
import com.dzirbel.kotify.repository.track.TrackViewModel
import com.dzirbel.kotify.ui.components.LinkedText
import com.dzirbel.kotify.ui.components.ToggleSaveButton
import com.dzirbel.kotify.ui.components.adapter.DividableProperty
import com.dzirbel.kotify.ui.components.adapter.SortOrder
import com.dzirbel.kotify.ui.components.adapter.SortableProperty
import com.dzirbel.kotify.ui.components.adapter.compareNullable
import com.dzirbel.kotify.ui.components.adapter.properties.PropertyByNumber
import com.dzirbel.kotify.ui.components.adapter.properties.PropertyByString
import com.dzirbel.kotify.ui.components.star.StarRating
import com.dzirbel.kotify.ui.components.table.Column
import com.dzirbel.kotify.ui.components.table.ColumnWidth
import com.dzirbel.kotify.ui.page.album.AlbumPage
import com.dzirbel.kotify.ui.page.artist.ArtistPage
import com.dzirbel.kotify.ui.pageStack
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.theme.surfaceBackground
import com.dzirbel.kotify.ui.util.applyIf
import com.dzirbel.kotify.ui.util.mutate
import com.dzirbel.kotify.util.formatDuration

open class TrackNameProperty<T>(private val toTrack: (T) -> TrackViewModel?) : PropertyByString<T>(title = "Name") {
    override val width = ColumnWidth.Weighted(weight = 1f)

    override fun toString(item: T) = toTrack(item)?.name

    companion object : TrackNameProperty<TrackViewModel>(toTrack = { it })
    object ForPlaylistTrack : TrackNameProperty<PlaylistTrackViewModel>(toTrack = { it.track })
}

open class TrackAlbumIndexProperty<T>(private val toTrack: (T) -> TrackViewModel) :
    PropertyByNumber<T>(title = "#", divisionRange = TRACK_INDEX_DIVISION_RANGE) {
    override fun toNumber(item: T) = toTrack(item).trackNumber

    companion object : TrackAlbumIndexProperty<TrackViewModel>(toTrack = { it })
}

open class TrackDurationProperty<T>(private val toTrack: (T) -> TrackViewModel?) :
    PropertyByNumber<T>(title = "Duration", divisionRange = DURATION_DIVISION_RANGE_MS) {
    override val cellAlignment = Alignment.TopEnd

    override fun toNumber(item: T) = toTrack(item)?.durationMs ?: 0

    override fun toString(item: T): String = formatDuration(toNumber(item))

    companion object : TrackDurationProperty<TrackViewModel>(toTrack = { it })
    object ForPlaylistTrack : TrackDurationProperty<PlaylistTrackViewModel>(toTrack = { it.track })
}

open class TrackArtistsProperty<T>(private val toTrack: (T) -> TrackViewModel?) :
    PropertyByString<T>(title = "Artist") {

    override val width = ColumnWidth.Weighted(weight = 1f)

    override fun toString(item: T): String? = toTrack(item)?.artists?.value?.joinToString()

    @Composable
    override fun Item(item: T) {
        toTrack(item)?.let { track ->
            val artists = track.artists.collectAsState().value

            LinkedText(
                key = artists,
                modifier = Modifier.padding(cellPadding),
                onClickLink = { link -> pageStack.mutate { to(ArtistPage(artistId = link)) } },
            ) {
                list(artists.orEmpty()) {
                    link(text = it.name, link = it.id)
                }
            }
        }
    }

    companion object : TrackArtistsProperty<TrackViewModel>(toTrack = { it })
    object ForPlaylistTrack : TrackArtistsProperty<PlaylistTrackViewModel>(toTrack = { it.track })
}

open class TrackAlbumProperty<T>(private val toTrack: (T) -> TrackViewModel?) :
    PropertyByString<T>(title = "Album") {

    override val width = ColumnWidth.Weighted(weight = 1f)

    override fun toString(item: T): String? = toTrack(item)?.album?.value?.name

    @Composable
    override fun Item(item: T) {
        val album = toTrack(item)?.album?.collectAsState()?.value

        if (album != null) {
            LinkedText(
                key = album,
                modifier = Modifier.padding(cellPadding),
                onClickLink = { link -> pageStack.mutate { to(AlbumPage(albumId = link)) } },
            ) {
                link(text = album.name, link = album.id)
            }
        }
    }

    companion object : TrackAlbumProperty<TrackViewModel>(toTrack = { it })
    object ForPlaylistTrack : TrackAlbumProperty<PlaylistTrackViewModel>(toTrack = { it.track })
}

open class TrackPopularityProperty<T>(private val toTrack: (T) -> TrackViewModel?) :
    PropertyByNumber<T>(title = "Popularity", divisionRange = POPULARITY_DIVISION_RANGE) {
    override val defaultSortOrder = SortOrder.DESCENDING
    override val defaultDivisionSortOrder = SortOrder.DESCENDING

    override val width: ColumnWidth = ColumnWidth.MatchHeader
    override val cellAlignment = Alignment.TopEnd

    override fun toNumber(item: T) = toTrack(item)?.popularity

    @Composable
    override fun Item(item: T) {
        val popularity = toNumber(item)
        val color = LocalColors.current.text.copy(alpha = ContentAlpha.disabled)

        LocalColors.current.WithSurface {
            Box(
                Modifier
                    .padding(Dimens.space3)
                    .surfaceBackground()
                    .height(Dimens.fontBodyDp)
                    .fillMaxWidth()
                    .border(width = Dimens.divider, color = color)
                    .applyIf(popularity == null) { alpha(ContentAlpha.disabled) },
            ) {
                if (popularity != null) {
                    Box(
                        @Suppress("MagicNumber")
                        Modifier
                            .background(color)
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = popularity / 100f),
                    )
                }
            }
        }
    }

    companion object : TrackPopularityProperty<TrackViewModel>(toTrack = { it })
    object ForPlaylistTrack : TrackPopularityProperty<PlaylistTrackViewModel>(toTrack = { it.track })
}

class TrackSavedProperty<T>(
    private val trackIdOf: (T) -> String?,
) : SortableProperty<T>, DividableProperty<T>, Column<T> {
    override val title = "Saved"
    override val width = ColumnWidth.Fill()

    override fun compare(sortOrder: SortOrder, first: T, second: T): Int {
        // TODO state here is awkward
        val firstId = trackIdOf(first)
        val secondId = trackIdOf(second)
        val firstSaved = firstId?.let { SavedTrackRepository.savedStateOf(it).value?.value }
        val secondSaved = secondId?.let { SavedTrackRepository.savedStateOf(it).value?.value }
        return sortOrder.compareNullable(firstSaved, secondSaved)
    }

    override fun divisionFor(element: T): Boolean? {
        // TODO state here is awkward
        return trackIdOf(element)?.let { SavedTrackRepository.savedStateOf(it).value?.value }
    }

    override fun compareDivisions(sortOrder: SortOrder, first: Any?, second: Any?): Int {
        return sortOrder.compareNullable(first as? Boolean, second as? Boolean)
    }

    override fun divisionTitle(division: Any?): String {
        return when (division as? Boolean) {
            true -> "Saved"
            false -> "Unsaved"
            null -> "Unknown"
        }
    }

    @Composable
    override fun Item(item: T) {
        trackIdOf(item)?.let { trackId ->
            ToggleSaveButton(
                repository = SavedTrackRepository,
                id = trackId,
                modifier = Modifier.padding(Dimens.space2),
            )
        }
    }
}

class TrackRatingProperty<T>(
    private val trackIdOf: (T) -> String?,
) : SortableProperty<T>, RatingDividableProperty<T>, Column<T> {
    override val title = "Rating"

    override val defaultSortOrder = SortOrder.DESCENDING
    override val defaultDivisionSortOrder = SortOrder.DESCENDING

    override val width: ColumnWidth = ColumnWidth.Fill()
    override val cellAlignment = Alignment.Center

    override fun compare(sortOrder: SortOrder, first: T, second: T): Int {
        return sortOrder.compareNullable(ratingOf(first), ratingOf(second))
    }

    override fun ratingOf(element: T): Double? {
        // TODO state here is awkward
        return trackIdOf(element)?.let { TrackRatingRepository.ratingStateOf(id = it).value?.ratingPercent }
    }

    @Composable
    override fun Item(item: T) {
        trackIdOf(item)?.let { trackId ->
            StarRating(
                rating = TrackRatingRepository.ratingStateOf(id = trackId).collectAsState().value,
                onRate = { rating -> TrackRatingRepository.rate(id = trackId, rating = rating) },
            )
        }
    }
}
