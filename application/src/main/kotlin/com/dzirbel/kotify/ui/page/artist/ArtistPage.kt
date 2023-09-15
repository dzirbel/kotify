package com.dzirbel.kotify.ui.page.artist

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.db.model.AlbumType
import com.dzirbel.kotify.repository.artist.ArtistAlbumViewModel
import com.dzirbel.kotify.repository.artist.ArtistViewModel
import com.dzirbel.kotify.repository.util.LazyTransactionStateFlow.Companion.requestBatched
import com.dzirbel.kotify.ui.LocalAlbumTracksRepository
import com.dzirbel.kotify.ui.LocalArtistAlbumsRepository
import com.dzirbel.kotify.ui.LocalArtistRepository
import com.dzirbel.kotify.ui.LocalRatingRepository
import com.dzirbel.kotify.ui.LocalSavedAlbumRepository
import com.dzirbel.kotify.ui.LocalSavedArtistRepository
import com.dzirbel.kotify.ui.album.AlbumCell
import com.dzirbel.kotify.ui.album.AlbumTypePicker
import com.dzirbel.kotify.ui.components.DividerSelector
import com.dzirbel.kotify.ui.components.Interpunct
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.PageLoadingSpinner
import com.dzirbel.kotify.ui.components.SortSelector
import com.dzirbel.kotify.ui.components.ToggleSaveButton
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
import com.dzirbel.kotify.ui.util.rememberSavedStates
import com.dzirbel.kotify.ui.util.rememberStates
import com.dzirbel.kotify.util.coroutines.mapIn
import com.dzirbel.kotify.util.coroutines.onEachIn
import com.dzirbel.kotify.util.immutable.countBy
import com.dzirbel.kotify.util.immutable.orEmpty
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

data class ArtistPage(val artistId: String) : Page {
    @Composable
    override fun PageScope.bind() {
        val artistAlbumsRepository = LocalArtistAlbumsRepository.current
        val ratingRepository = LocalRatingRepository.current

        val artist = LocalArtistRepository.current.stateOf(artistId).collectAsState().value?.cachedValue

        val displayedAlbumTypes = remember { mutableStateOf(persistentSetOf(AlbumType.ALBUM)) }

        val imageSize = Dimens.contentImage.toImageSize()
        val artistAlbums = rememberListAdapterState(
            key = artistId,
            defaultSort = AlbumReleaseDateProperty.ForArtistAlbum,
            defaultFilter = filterFor(displayedAlbumTypes.value),
            source = { scope ->
                artistAlbumsRepository.stateOf(id = artistId)
                    .mapIn(scope) { it?.cachedValue }
                    .onEachIn(scope) { artistAlbum ->
                        artistAlbum?.requestBatched(
                            transactionName = { "load $it artist album images for $imageSize" },
                            extractor = { it.album.imageUrlFor(imageSize) },
                        )
                    }
            },
        )

        LocalSavedAlbumRepository.current.rememberSavedStates(artistAlbums.value) { it.album.id }
        LocalAlbumTracksRepository.current.rememberStates(ids = artistAlbums.value.map { it.album.id })

        val scope = rememberCoroutineScope()
        val artistAlbumProperties = remember(artistAlbums.value) {
            persistentListOf(
                AlbumNameProperty.ForArtistAlbum,
                AlbumReleaseDateProperty.ForArtistAlbum,
                AlbumTypeDividableProperty.ForArtistAlbum,
                AlbumRatingProperty.ForArtistAlbum(
                    ratings = artistAlbums.value.associate { artistAlbum ->
                        val albumId = artistAlbum.album.id
                        albumId to ratingRepository.averageRatingStateOfAlbum(albumId = albumId, scope = scope)
                    },
                ),
                AlbumTotalTracksProperty.ForArtistAlbum,
            )
        }

        DisplayVerticalScrollPage(
            title = artist?.name,
            header = {
                ArtistPageHeader(
                    artistId = artistId,
                    artist = artist,
                    albums = artistAlbums,
                    artistAlbumProperties = artistAlbumProperties,
                    displayedAlbumTypes = displayedAlbumTypes.value,
                    setDisplayedAlbumTypes = { types ->
                        displayedAlbumTypes.value = types
                        artistAlbums.withFilter(filterFor(types))
                    },
                )
            },
        ) {
            if (artistAlbums.derived { it.hasElements }.value) {
                Grid(
                    elements = artistAlbums.value,
                    edgePadding = PaddingValues(
                        start = Dimens.space5 - Dimens.space3,
                        end = Dimens.space5 - Dimens.space3,
                        bottom = Dimens.space3,
                    ),
                ) { _, artistAlbum ->
                    AlbumCell(
                        album = artistAlbum.album,
                        onClick = { pageStack.mutate { to(AlbumPage(albumId = artistAlbum.album.id)) } },
                    )
                }
            } else {
                PageLoadingSpinner()
            }
        }
    }
}

@Composable
private fun ArtistPageHeader(
    artistId: String,
    artist: ArtistViewModel?,
    albums: ListAdapterState<ArtistAlbumViewModel>,
    artistAlbumProperties: PersistentList<AdapterProperty<ArtistAlbumViewModel>>,
    displayedAlbumTypes: PersistentSet<AlbumType>,
    setDisplayedAlbumTypes: (PersistentSet<AlbumType>) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.space5, vertical = Dimens.space4),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.space3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(artist?.name.orEmpty(), style = MaterialTheme.typography.h4)

                ToggleSaveButton(
                    repository = LocalSavedArtistRepository.current,
                    id = artistId,
                    size = Dimens.iconMedium,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                    if (albums.derived { it.hasElements }.value) {
                        val size = albums.derived { it.size }.value
                        Text(
                            text = "$size albums",
                            modifier = Modifier.padding(end = Dimens.space2),
                        )

                        Interpunct()
                    }

                    if (artist != null) {
                        InvalidateButton(
                            repository = LocalArtistRepository.current,
                            id = artist.id,
                            entityName = "Artist",
                            contentPadding = PaddingValues(all = Dimens.space2),
                        )

                        Interpunct()

                        InvalidateButton(
                            repository = LocalArtistAlbumsRepository.current,
                            id = artist.id,
                            entityName = "Albums",
                            contentPadding = PaddingValues(all = Dimens.space2),
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space3)) {
            val albumTypeCounts = albums.derived { it.countBy { artistAlbum -> artistAlbum.albumGroup } }.value
            AlbumTypePicker(
                albumTypeCounts = albumTypeCounts,
                albumTypes = displayedAlbumTypes,
                onSelectAlbumTypes = { setDisplayedAlbumTypes(it) },
            )

            DividerSelector(
                dividableProperties = artistAlbumProperties.dividableProperties(),
                currentDivider = albums.value.divider,
                onSelectDivider = { albums.withDivider(it) },
            )

            SortSelector(
                sortableProperties = artistAlbumProperties.sortableProperties(),
                sorts = albums.value.sorts.orEmpty(),
                onSetSort = { albums.withSort(it) },
            )
        }
    }
}

private fun filterFor(albumTypes: Set<AlbumType>): ((ArtistAlbumViewModel) -> Boolean)? {
    return if (albumTypes.isNotEmpty()) {
        { album -> albumTypes.contains(album.albumGroup) }
    } else {
        null
    }
}
