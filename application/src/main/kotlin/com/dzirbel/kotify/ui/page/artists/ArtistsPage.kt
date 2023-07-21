package com.dzirbel.kotify.ui.page.artists

import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.onClick
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.repository2.artist.ArtistRepository
import com.dzirbel.kotify.repository2.artist.SavedArtistRepository
import com.dzirbel.kotify.repository2.player.Player
import com.dzirbel.kotify.repository2.rating.TrackRatingRepository
import com.dzirbel.kotify.ui.components.DividerSelector
import com.dzirbel.kotify.ui.components.Flow
import com.dzirbel.kotify.ui.components.Interpunct
import com.dzirbel.kotify.ui.components.LibraryInvalidateButton
import com.dzirbel.kotify.ui.components.LoadedImage
import com.dzirbel.kotify.ui.components.PageLoadingSpinner
import com.dzirbel.kotify.ui.components.Pill
import com.dzirbel.kotify.ui.components.PlayButton
import com.dzirbel.kotify.ui.components.SmallAlbumCell
import com.dzirbel.kotify.ui.components.SortSelector
import com.dzirbel.kotify.ui.components.ToggleSaveButton
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.components.adapter.AdapterProperty
import com.dzirbel.kotify.ui.components.adapter.ListAdapter
import com.dzirbel.kotify.ui.components.adapter.ListAdapterState
import com.dzirbel.kotify.ui.components.adapter.dividableProperties
import com.dzirbel.kotify.ui.components.adapter.rememberListAdapterState
import com.dzirbel.kotify.ui.components.adapter.sortableProperties
import com.dzirbel.kotify.ui.components.grid.Grid
import com.dzirbel.kotify.ui.components.liveRelativeDateText
import com.dzirbel.kotify.ui.components.star.AverageStarRating
import com.dzirbel.kotify.ui.components.star.RatingHistogram
import com.dzirbel.kotify.ui.framework.Page
import com.dzirbel.kotify.ui.framework.VerticalScrollPage
import com.dzirbel.kotify.ui.page.album.AlbumPage
import com.dzirbel.kotify.ui.page.artist.ArtistPage
import com.dzirbel.kotify.ui.pageStack
import com.dzirbel.kotify.ui.properties.AlbumNameProperty
import com.dzirbel.kotify.ui.properties.ArtistNameProperty
import com.dzirbel.kotify.ui.properties.ArtistPopularityProperty
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.derived
import com.dzirbel.kotify.ui.util.instrumentation.instrument
import com.dzirbel.kotify.ui.util.mutate
import com.dzirbel.kotify.util.combinedStateWhenAllNotNull
import com.dzirbel.kotify.util.flatMapLatestIn
import com.dzirbel.kotify.util.immutable.orEmpty
import com.dzirbel.kotify.util.produceTransactionState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow

private val properties: PersistentList<AdapterProperty<Artist>> = persistentListOf(
    ArtistNameProperty,
    ArtistPopularityProperty,
    // ArtistRatingProperty(ratings = artistRatings), TODO add ArtistRatingProperty back
)

object ArtistsPage : Page<Unit>() {
    @Composable
    override fun BoxScope.bind(visible: Boolean) {
        val artistsAdapter = rememberListAdapterState(defaultSort = ArtistNameProperty) { scope ->
            SavedArtistRepository.library.flatMapLatestIn(scope) { library ->
                library?.ids
                    ?.let { artistIds ->
                        ArtistRepository.statesOf(artistIds).combinedStateWhenAllNotNull { it?.cachedValue }
                    }
                    ?: MutableStateFlow(null)
            }
        }

        val selectedArtistIndex = remember { mutableStateOf<Int?>(null) }

        VerticalScrollPage(
            visible = visible,
            onHeaderVisibilityChanged = { headerVisible -> navigationTitleState.targetState = !headerVisible },
            header = {
                ArtistsPageHeader(artistsAdapter = artistsAdapter)
            },
            content = {
                if (artistsAdapter.derived { it.hasElements }.value) {
                    Grid(
                        elements = artistsAdapter.value,
                        edgePadding = PaddingValues(
                            start = Dimens.space5 - Dimens.space3,
                            end = Dimens.space5 - Dimens.space3,
                            bottom = Dimens.space3,
                        ),
                        selectedElementIndex = selectedArtistIndex.value,
                        detailInsertContent = { _, artist ->
                            ArtistDetailInsert(artist = artist)
                        },
                    ) { index, artist ->
                        ArtistCell(
                            artist = artist,
                            onRightClick = {
                                selectedArtistIndex.value = index.takeIf { it != selectedArtistIndex.value }
                            },
                        )
                    }
                } else {
                    PageLoadingSpinner()
                }
            },
        )
    }

    override fun titleFor(data: Unit) = "Saved Artists"
}

@Composable
private fun ArtistsPageHeader(artistsAdapter: ListAdapterState<Artist>) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.space5, vertical = Dimens.space4),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("Artists", style = MaterialTheme.typography.h4)

            if (artistsAdapter.derived { it.hasElements }.value) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                        val size = artistsAdapter.derived { it.size }.value
                        Text(
                            text = "$size saved artists",
                            modifier = Modifier.padding(end = Dimens.space2),
                        )

                        Interpunct()

                        LibraryInvalidateButton(
                            savedRepository = SavedArtistRepository,
                            contentPadding = PaddingValues(all = Dimens.space2),
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space3)) {
            DividerSelector(
                dividableProperties = properties.dividableProperties(),
                currentDivider = artistsAdapter.derived { it.divider }.value,
                onSelectDivider = artistsAdapter::withDivider,
            )

            SortSelector(
                sortableProperties = properties.sortableProperties(),
                sorts = artistsAdapter.derived { it.sorts.orEmpty() }.value,
                onSetSort = artistsAdapter::withSort,
            )
        }
    }
}

@Composable
private fun ArtistCell(artist: Artist, onRightClick: () -> Unit) {
    Column(
        Modifier
            .instrument()
            .onClick(matcher = PointerMatcher.mouse(PointerButton.Primary)) {
                pageStack.mutate { to(ArtistPage(artistId = artist.id.value)) }
            }
            .onClick(matcher = PointerMatcher.mouse(PointerButton.Secondary), onClick = onRightClick)
            .padding(Dimens.space3),
    ) {
        LoadedImage(imageProperty = artist.largestImage, modifier = Modifier.align(Alignment.CenterHorizontally))

        VerticalSpacer(Dimens.space3)

        Row(
            modifier = Modifier.widthIn(max = Dimens.contentImage),
            horizontalArrangement = Arrangement.spacedBy(Dimens.space2),
        ) {
            Text(text = artist.name, modifier = Modifier.weight(1f))

            ToggleSaveButton(repository = SavedArtistRepository, id = artist.id.value)

            PlayButton(context = Player.PlayContext.artist(artist), size = Dimens.iconSmall)
        }

        // TODO have the TrackRatingRepository provide a state by artist directly
        val artistTrackIds = artist.produceTransactionState("load artist tracks") { trackIds.live }.value
        val averageRating = artistTrackIds?.let {
            remember(artist.id.value) { TrackRatingRepository.averageRatingStateOf(artistTrackIds) }
        }
            ?.collectAsState()
            ?.value
        AverageStarRating(averageRating = averageRating)
    }
}

private const val DETAILS_COLUMN_WEIGHT = 0.3f
private const val DETAILS_ALBUMS_WEIGHT = 0.7f

@Composable
private fun ArtistDetailInsert(artist: Artist) {
    Row(modifier = Modifier.padding(Dimens.space4), horizontalArrangement = Arrangement.spacedBy(Dimens.space3)) {
        LoadedImage(imageProperty = artist.largestImage)

        Column(
            modifier = Modifier.weight(weight = DETAILS_COLUMN_WEIGHT),
            verticalArrangement = Arrangement.spacedBy(Dimens.space2),
        ) {
            Text(text = artist.name, style = MaterialTheme.typography.h5)

            ArtistRepository.stateOf(id = artist.id.value)
                .collectAsState()
                .derived { it?.cacheTime?.toEpochMilli() }
                .value
                ?.let { timestamp ->
                    Text(liveRelativeDateText(timestamp = timestamp) { "Saved $it" })
                }

            artist.produceTransactionState("load artist genres") { genres.live }.value?.let { genres ->
                Flow {
                    for (genre in genres) {
                        Pill(text = genre.name)
                    }
                }
            }

            // TODO have the TrackRatingRepository provide a state by artist directly
            val artistTrackIds = artist.produceTransactionState("load artist tracks") { trackIds.live }.value
            val averageRating = artistTrackIds?.let { TrackRatingRepository.averageRatingStateOf(ids = it) }?.value
            if (averageRating != null) {
                RatingHistogram(averageRating)
            }
        }

        artist.artistAlbums.produceTransactionState(
            onLive = { artistsAlbums ->
                artistsAlbums.onEach { artistAlbum ->
                    artistAlbum.album.loadToCache()
                    artistAlbum.album.live.largestImage.loadToCache()
                }
            },
        ).value?.let { albums ->
            val adapter = remember { ListAdapter.of(elements = albums, defaultSort = AlbumNameProperty.ForArtistAlbum) }
            Grid(
                modifier = Modifier.weight(DETAILS_ALBUMS_WEIGHT),
                elements = adapter,
            ) { _, artistAlbum ->
                SmallAlbumCell(
                    album = artistAlbum.album.cached,
                    onClick = { pageStack.mutate { to(AlbumPage(albumId = artistAlbum.albumId.value)) } },
                )
            }
        }
    }
}
