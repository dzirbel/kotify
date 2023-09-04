package com.dzirbel.kotify.ui.page.artists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.repository.artist.ArtistAlbumsRepository
import com.dzirbel.kotify.repository.artist.ArtistRepository
import com.dzirbel.kotify.repository.artist.ArtistTracksRepository
import com.dzirbel.kotify.repository.artist.ArtistViewModel
import com.dzirbel.kotify.repository.artist.SavedArtistRepository
import com.dzirbel.kotify.repository.rating.TrackRatingRepository
import com.dzirbel.kotify.repository.util.LazyTransactionStateFlow.Companion.requestBatched
import com.dzirbel.kotify.ui.album.SmallAlbumCell
import com.dzirbel.kotify.ui.artist.ArtistCell
import com.dzirbel.kotify.ui.components.DividerSelector
import com.dzirbel.kotify.ui.components.Flow
import com.dzirbel.kotify.ui.components.Interpunct
import com.dzirbel.kotify.ui.components.LibraryInvalidateButton
import com.dzirbel.kotify.ui.components.LoadedImage
import com.dzirbel.kotify.ui.components.PageLoadingSpinner
import com.dzirbel.kotify.ui.components.Pill
import com.dzirbel.kotify.ui.components.SortSelector
import com.dzirbel.kotify.ui.components.adapter.AdapterProperty
import com.dzirbel.kotify.ui.components.adapter.ListAdapterState
import com.dzirbel.kotify.ui.components.adapter.dividableProperties
import com.dzirbel.kotify.ui.components.adapter.rememberListAdapterState
import com.dzirbel.kotify.ui.components.adapter.sortableProperties
import com.dzirbel.kotify.ui.components.grid.Grid
import com.dzirbel.kotify.ui.components.liveRelativeDateText
import com.dzirbel.kotify.ui.components.star.RatingHistogram
import com.dzirbel.kotify.ui.components.toImageSize
import com.dzirbel.kotify.ui.page.Page
import com.dzirbel.kotify.ui.page.PageScope
import com.dzirbel.kotify.ui.page.album.AlbumPage
import com.dzirbel.kotify.ui.page.artist.ArtistPage
import com.dzirbel.kotify.ui.pageStack
import com.dzirbel.kotify.ui.properties.ArtistNameProperty
import com.dzirbel.kotify.ui.properties.ArtistPopularityProperty
import com.dzirbel.kotify.ui.properties.ArtistRatingProperty
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.derived
import com.dzirbel.kotify.ui.util.mutate
import com.dzirbel.kotify.ui.util.rememberArtistTracksStates
import com.dzirbel.kotify.util.coroutines.combinedStateWhenAllNotNull
import com.dzirbel.kotify.util.coroutines.flatMapLatestIn
import com.dzirbel.kotify.util.coroutines.mapIn
import com.dzirbel.kotify.util.coroutines.onEachIn
import com.dzirbel.kotify.util.coroutines.runningFoldIn
import com.dzirbel.kotify.util.immutable.orEmpty
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow

val artistCellImageSize = Dimens.contentImage

data object ArtistsPage : Page {
    @Composable
    override fun PageScope.bind() {
        val scope = rememberCoroutineScope()

        // accumulate saved artist IDs, never removing them from the library so that the artist does not disappear from
        // the grid when removed (to make it easy to add them back if it was an accident)
        val displayedLibraryFlow = remember {
            SavedArtistRepository.library
                .runningFoldIn(scope) { accumulator, value -> value?.plus(accumulator?.ids) }
        }

        val imageSize = artistCellImageSize.toImageSize()
        val artistsAdapter = rememberListAdapterState(defaultSort = ArtistNameProperty, scope = scope) {
            displayedLibraryFlow.flatMapLatestIn(scope) { library ->
                if (library == null) {
                    MutableStateFlow(null)
                } else {
                    ArtistRepository.statesOf(library.ids)
                        .combinedStateWhenAllNotNull { it?.cachedValue }
                        .onEachIn(scope) { artists ->
                            artists?.requestBatched(
                                transactionName = { "load $it artist images for $artistCellImageSize" },
                                extractor = { it.imageUrlFor(imageSize) },
                            )
                        }
                }
            }
        }

        val artistIds = displayedLibraryFlow.collectAsState().value?.ids
        ArtistTracksRepository.rememberArtistTracksStates(artistIds)

        val artistProperties = remember(artistIds) {
            persistentListOf(
                ArtistNameProperty,
                ArtistPopularityProperty,
                ArtistRatingProperty(
                    ratings = artistIds?.associateWith { artistId ->
                        TrackRatingRepository.averageRatingStateOfArtist(artistId = artistId, scope = scope)
                    },
                ),
            )
        }

        DisplayVerticalScrollPage(
            title = "Saved Artists",
            header = {
                ArtistsPageHeader(artistsAdapter = artistsAdapter, artistProperties = artistProperties)
            },
        ) {
            if (artistsAdapter.derived { it.hasElements }.value) {
                var selectedArtistIndex by remember { mutableStateOf<Int?>(null) }

                Grid(
                    elements = artistsAdapter.value,
                    edgePadding = PaddingValues(
                        start = Dimens.space5 - Dimens.space3,
                        end = Dimens.space5 - Dimens.space3,
                        bottom = Dimens.space3,
                    ),
                    selectedElementIndex = selectedArtistIndex,
                    detailInsertContent = { _, artist ->
                        ArtistDetailInsert(artist = artist)
                    },
                    cellContent = { index, artist ->
                        ArtistCell(
                            artist = artist,
                            imageSize = artistCellImageSize,
                            onClick = {
                                pageStack.mutate { to(ArtistPage(artistId = artist.id)) }
                            },
                            onMiddleClick = {
                                selectedArtistIndex = index.takeIf { it != selectedArtistIndex }
                            },
                        )
                    },
                )
            } else {
                PageLoadingSpinner()
            }
        }
    }
}

@Composable
private fun ArtistsPageHeader(
    artistsAdapter: ListAdapterState<ArtistViewModel>,
    artistProperties: PersistentList<AdapterProperty<ArtistViewModel>>,
) {
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
                dividableProperties = artistProperties.dividableProperties(),
                currentDivider = artistsAdapter.derived { it.divider }.value,
                onSelectDivider = artistsAdapter::withDivider,
            )

            SortSelector(
                sortableProperties = artistProperties.sortableProperties(),
                sorts = artistsAdapter.derived { it.sorts.orEmpty() }.value,
                onSetSort = artistsAdapter::withSort,
            )
        }
    }
}

private const val DETAILS_COLUMN_WEIGHT = 0.3f
private const val DETAILS_ALBUMS_WEIGHT = 0.7f

@Composable
private fun ArtistDetailInsert(artist: ArtistViewModel) {
    Row(modifier = Modifier.padding(Dimens.space4), horizontalArrangement = Arrangement.spacedBy(Dimens.space3)) {
        LoadedImage(key = artist.id) { size -> artist.imageUrlFor(size) }

        Column(
            modifier = Modifier.weight(weight = DETAILS_COLUMN_WEIGHT),
            verticalArrangement = Arrangement.spacedBy(Dimens.space2),
        ) {
            Text(text = artist.name, style = MaterialTheme.typography.h5)

            ArtistRepository.stateOf(id = artist.id)
                .collectAsState()
                .derived { it?.cacheTime?.toEpochMilli() }
                .value
                ?.let { timestamp ->
                    Text(liveRelativeDateText(timestamp = timestamp) { "Saved $it" })
                }

            artist.genres.collectAsState().value?.let { genres ->
                Flow {
                    for (genre in genres) {
                        Pill(text = genre.name)
                    }
                }
            }

            val scope = rememberCoroutineScope()
            val averageRating = remember(artist.id) {
                TrackRatingRepository.averageRatingStateOfArtist(artistId = artist.id, scope = scope)
            }
                .collectAsState()
                .value

            RatingHistogram(averageRating)
        }

        // TODO add loading state
        val adapter = rememberListAdapterState(key = artist.id) { scope ->
            ArtistAlbumsRepository.stateOf(artist.id).mapIn(scope) { it?.cachedValue }
        }

        Grid(
            modifier = Modifier.weight(DETAILS_ALBUMS_WEIGHT),
            elements = adapter.value,
        ) { _, artistAlbum ->
            // TODO batch load images?
            SmallAlbumCell(
                album = artistAlbum.album,
                onClick = { pageStack.mutate { to(AlbumPage(albumId = artistAlbum.album.id)) } },
            )
        }
    }
}
