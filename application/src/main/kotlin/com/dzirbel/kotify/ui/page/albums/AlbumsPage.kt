package com.dzirbel.kotify.ui.page.albums

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.repository.CacheState
import com.dzirbel.kotify.repository.album.AlbumViewModel
import com.dzirbel.kotify.repository.util.LazyTransactionStateFlow.Companion.requestBatched
import com.dzirbel.kotify.ui.LocalAlbumRepository
import com.dzirbel.kotify.ui.LocalAlbumTracksRepository
import com.dzirbel.kotify.ui.LocalRatingRepository
import com.dzirbel.kotify.ui.LocalSavedAlbumRepository
import com.dzirbel.kotify.ui.album.AlbumCell
import com.dzirbel.kotify.ui.components.DividerSelector
import com.dzirbel.kotify.ui.components.Interpunct
import com.dzirbel.kotify.ui.components.LibraryInvalidateButton
import com.dzirbel.kotify.ui.components.PageLoadingSpinner
import com.dzirbel.kotify.ui.components.SortSelector
import com.dzirbel.kotify.ui.components.adapter.AdapterProperty
import com.dzirbel.kotify.ui.components.adapter.ListAdapterState
import com.dzirbel.kotify.ui.components.adapter.dividableProperties
import com.dzirbel.kotify.ui.components.adapter.rememberListAdapterState
import com.dzirbel.kotify.ui.components.adapter.sortableProperties
import com.dzirbel.kotify.ui.components.grid.Grid
import com.dzirbel.kotify.ui.components.toImageSize
import com.dzirbel.kotify.ui.page.Page
import com.dzirbel.kotify.ui.page.PageScope
import com.dzirbel.kotify.ui.page.album.AlbumPage
import com.dzirbel.kotify.ui.pageStack
import com.dzirbel.kotify.ui.properties.AlbumNameProperty
import com.dzirbel.kotify.ui.properties.AlbumRatingProperty
import com.dzirbel.kotify.ui.properties.AlbumReleaseDateProperty
import com.dzirbel.kotify.ui.properties.AlbumTotalTracksProperty
import com.dzirbel.kotify.ui.properties.AlbumTypeDividableProperty
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.derived
import com.dzirbel.kotify.ui.util.mutate
import com.dzirbel.kotify.ui.util.rememberStates
import com.dzirbel.kotify.util.coroutines.combinedStateWhenAllNotNull
import com.dzirbel.kotify.util.coroutines.flatMapLatestIn
import com.dzirbel.kotify.util.coroutines.onEachIn
import com.dzirbel.kotify.util.coroutines.runningFoldIn
import com.dzirbel.kotify.util.immutable.orEmpty
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow

val albumCellImageSize = Dimens.contentImage

data object AlbumsPage : Page {
    @Composable
    override fun PageScope.bind() {
        val scope = rememberCoroutineScope()

        val savedAlbumRepository = LocalSavedAlbumRepository.current
        val albumRepository = LocalAlbumRepository.current
        val ratingRepository = LocalRatingRepository.current

        val displayedLibraryFlow = remember {
            // TODO do not propagate error/not found states
            savedAlbumRepository.library.runningFoldIn(scope) { accumulator, value ->
                value?.map { it.plus(accumulator?.cachedValue?.ids) }
            }
        }

        val imageSize = albumCellImageSize.toImageSize()
        val albumsAdapter = rememberListAdapterState(defaultSort = AlbumNameProperty, scope = scope) {
            displayedLibraryFlow.flatMapLatestIn(scope) { cacheState ->
                val ids = cacheState?.cachedValue?.ids
                if (ids == null) {
                    MutableStateFlow(if (cacheState is CacheState.Error) emptyList() else null)
                } else {
                    albumRepository.statesOf(ids)
                        .combinedStateWhenAllNotNull { it?.cachedValue }
                        .onEachIn(scope) { albums ->
                            albums?.requestBatched(
                                transactionName = { "load $it albums images for $albumCellImageSize" },
                                extractor = { it.imageUrlFor(imageSize) },
                            )
                        }
                }
            }
        }

        val albumIds = displayedLibraryFlow.collectAsState().value?.cachedValue?.ids
        LocalAlbumTracksRepository.current.rememberStates(albumIds)

        val albumProperties = remember(albumIds) {
            persistentListOf(
                AlbumNameProperty,
                AlbumRatingProperty(
                    ratings = albumIds?.associateWith { albumId ->
                        ratingRepository.averageRatingStateOfAlbum(albumId = albumId, scope = scope)
                    },
                ),
                AlbumReleaseDateProperty,
                AlbumTypeDividableProperty,
                AlbumTotalTracksProperty,
            )
        }

        DisplayVerticalScrollPage(
            title = "Saved Albums",
            header = {
                AlbumsPageHeader(albumsAdapter = albumsAdapter, albumProperties = albumProperties)
            },
        ) {
            if (albumsAdapter.derived { it.hasElements }.value) {
                Grid(
                    elements = albumsAdapter.value,
                    edgePadding = PaddingValues(
                        start = Dimens.space5 - Dimens.space3,
                        end = Dimens.space5 - Dimens.space3,
                        bottom = Dimens.space3,
                    ),
                    cellContent = { _, album ->
                        AlbumCell(
                            album = album,
                            onClick = { pageStack.mutate { to(AlbumPage(albumId = album.id)) } },
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
private fun AlbumsPageHeader(
    albumsAdapter: ListAdapterState<AlbumViewModel>,
    albumProperties: PersistentList<AdapterProperty<AlbumViewModel>>,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.space5, vertical = Dimens.space4),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("Albums", style = MaterialTheme.typography.h4)

            if (albumsAdapter.derived { it.hasElements }.value) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space2),
                ) {
                    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                        val size = albumsAdapter.derived { it.size }.value
                        Text("$size saved albums")

                        Interpunct()
                    }

                    LibraryInvalidateButton(LocalSavedAlbumRepository.current)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space3)) {
            DividerSelector(
                dividableProperties = albumProperties.dividableProperties(),
                currentDivider = albumsAdapter.derived { it.divider }.value,
                onSelectDivider = albumsAdapter::withDivider,
            )

            SortSelector(
                sortableProperties = albumProperties.sortableProperties(),
                sorts = albumsAdapter.derived { it.sorts.orEmpty() }.value,
                onSetSort = albumsAdapter::withSort,
            )
        }
    }
}
