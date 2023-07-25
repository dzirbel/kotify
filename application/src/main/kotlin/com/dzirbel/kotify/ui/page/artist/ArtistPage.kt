package com.dzirbel.kotify.ui.page.artist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.db.model.AlbumType
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.ArtistAlbum
import com.dzirbel.kotify.repository.artist.ArtistAlbumsRepository
import com.dzirbel.kotify.repository.artist.ArtistRepository
import com.dzirbel.kotify.ui.components.AlbumCell
import com.dzirbel.kotify.ui.components.AlbumTypePicker
import com.dzirbel.kotify.ui.components.DividerSelector
import com.dzirbel.kotify.ui.components.Interpunct
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.PageLoadingSpinner
import com.dzirbel.kotify.ui.components.SortSelector
import com.dzirbel.kotify.ui.components.adapter.ListAdapterState
import com.dzirbel.kotify.ui.components.adapter.dividableProperties
import com.dzirbel.kotify.ui.components.adapter.rememberListAdapterState
import com.dzirbel.kotify.ui.components.adapter.sortableProperties
import com.dzirbel.kotify.ui.components.grid.Grid
import com.dzirbel.kotify.ui.framework.Page
import com.dzirbel.kotify.ui.framework.VerticalScrollPage
import com.dzirbel.kotify.ui.page.album.AlbumPage
import com.dzirbel.kotify.ui.pageStack
import com.dzirbel.kotify.ui.properties.AlbumNameProperty
import com.dzirbel.kotify.ui.properties.AlbumReleaseDateProperty
import com.dzirbel.kotify.ui.properties.AlbumTypeDividableProperty
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.derived
import com.dzirbel.kotify.ui.util.mutate
import com.dzirbel.kotify.util.countsBy
import com.dzirbel.kotify.util.immutable.orEmpty
import com.dzirbel.kotify.util.mapIn
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

private val artistAlbumProperties = persistentListOf(
    AlbumNameProperty.ForArtistAlbum,
    AlbumReleaseDateProperty.ForArtistAlbum,
    AlbumTypeDividableProperty.ForArtistAlbum,
    // TODO AlbumRatingProperty.ForArtistAlbum(ratings = albumRatings),
)

data class ArtistPage(val artistId: String) : Page<String?>() {
    @Composable
    override fun BoxScope.bind(visible: Boolean): String? {
        val artist = ArtistRepository.stateOf(artistId).collectAsState().value?.cachedValue

        val displayedAlbumTypes = remember { mutableStateOf(persistentSetOf(AlbumType.ALBUM)) }

        val artistAlbums = rememberListAdapterState(
            key = artistId,
            defaultSort = AlbumReleaseDateProperty.ForArtistAlbum,
            defaultFilter = filterFor(displayedAlbumTypes.value), // TODO lazy?
            source = { scope ->
                ArtistAlbumsRepository.stateOf(id = artistId).mapIn(scope) { it?.cachedValue }
            },
        )

        VerticalScrollPage(
            visible = visible,
            onHeaderVisibilityChanged = { headerVisible -> navigationTitleState.targetState = !headerVisible },
            header = {
                ArtistPageHeader(
                    artist = artist,
                    albums = artistAlbums,
                    displayedAlbumTypes = displayedAlbumTypes.value,
                    setDisplayedAlbumTypes = { types ->
                        displayedAlbumTypes.value = types
                        artistAlbums.withFilter(filterFor(types))
                    },
                )
            },
            content = {
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
                            album = artistAlbum.album.cached,
                            onClick = { pageStack.mutate { to(AlbumPage(albumId = artistAlbum.albumId.value)) } },
                        )
                    }
                } else {
                    PageLoadingSpinner()
                }
            },
        )

        return artist?.name
    }

    override fun titleFor(data: String?) = data
}

@Composable
private fun ArtistPageHeader(
    artist: Artist?,
    albums: ListAdapterState<ArtistAlbum>,
    displayedAlbumTypes: PersistentSet<AlbumType>,
    setDisplayedAlbumTypes: (PersistentSet<AlbumType>) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.space5, vertical = Dimens.space4),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(artist?.name.orEmpty(), style = MaterialTheme.typography.h4)

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
                            repository = ArtistRepository,
                            id = artist.id.value,
                            entityName = "Artist",
                            contentPadding = PaddingValues(all = Dimens.space2),
                        )

                        Interpunct()

                        InvalidateButton(
                            repository = ArtistAlbumsRepository,
                            id = artist.id.value,
                            entityName = "Albums",
                            contentPadding = PaddingValues(all = Dimens.space2),
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space3)) {
            val albumTypeCounts = albums.derived { it.countsBy { artistAlbum -> artistAlbum.albumGroup } }.value
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

private fun filterFor(albumTypes: Set<AlbumType>): ((ArtistAlbum) -> Boolean)? {
    return if (albumTypes.isNotEmpty()) {
        { album -> albumTypes.contains(album.albumGroup) }
    } else {
        null
    }
}
