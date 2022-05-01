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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.ui.components.AlbumCell
import com.dzirbel.kotify.ui.components.AlbumTypePicker
import com.dzirbel.kotify.ui.components.Interpunct
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.PageLoadingSpinner
import com.dzirbel.kotify.ui.components.adapter.DividerSelector
import com.dzirbel.kotify.ui.components.adapter.SortSelector
import com.dzirbel.kotify.ui.components.adapter.dividableProperties
import com.dzirbel.kotify.ui.components.adapter.sortableProperties
import com.dzirbel.kotify.ui.components.grid.Grid
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.util.countsBy

@Composable
fun ArtistPageHeader(presenter: ArtistPresenter, state: ArtistPresenter.ViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.space5, vertical = Dimens.space4),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(state.artist?.value?.name.orEmpty(), style = MaterialTheme.typography.h4)

            Row(verticalAlignment = Alignment.CenterVertically) {
                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                    if (state.artistAlbums.hasElements) {
                        Text(
                            text = "${state.artistAlbums.size} albums",
                            modifier = Modifier.padding(end = Dimens.space2),
                        )

                        Interpunct()
                    }

                    if (state.artist != null) {
                        InvalidateButton(
                            contentPadding = PaddingValues(all = Dimens.space2),
                            refreshing = state.refreshingArtist,
                            updated = state.artist.value.fullUpdatedTime?.toEpochMilli(),
                            updatedFormat = { "Artist synced $it" },
                            updatedFallback = "Artist never synced",
                            onClick = {
                                presenter.emitAsync(ArtistPresenter.Event.RefreshArtist)
                            },
                        )

                        Interpunct()

                        InvalidateButton(
                            contentPadding = PaddingValues(all = Dimens.space2),
                            refreshing = state.refreshingArtistAlbums,
                            updated = state.artist.value.albumsFetched?.toEpochMilli(),
                            updatedFormat = { "Albums synced $it" },
                            updatedFallback = "Albums never synced",
                            onClick = {
                                presenter.emitAsync(ArtistPresenter.Event.LoadArtistAlbums(invalidate = true))
                            },
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space3)) {
            AlbumTypePicker(
                albumTypeCounts = state.artistAlbums.countsBy { it.albumGroup },
                albumTypes = state.displayedAlbumTypes,
                onSelectAlbumTypes = { albumTypes ->
                    presenter.emitAsync(ArtistPresenter.Event.SetDisplayedAlbumTypes(albumTypes))
                },
            )

            DividerSelector(
                dividableProperties = state.artistAlbumProperties.dividableProperties(),
                currentDivider = state.artistAlbums.divider,
                onSelectDivider = { presenter.emitAsync(ArtistPresenter.Event.SetDivider(divider = it)) },
            )

            SortSelector(
                sortableProperties = state.artistAlbumProperties.sortableProperties(),
                sorts = state.artistAlbums.sorts.orEmpty(),
                onSetSort = { presenter.emitAsync(ArtistPresenter.Event.SetSorts(sorts = it)) },
            )
        }
    }
}

@Composable
fun ArtistPageContent(presenter: ArtistPresenter, state: ArtistPresenter.ViewModel) {
    if (state.artistAlbums.hasElements) {
        Grid(
            elements = state.artistAlbums,
            edgePadding = PaddingValues(
                start = Dimens.space5 - Dimens.space3,
                end = Dimens.space5 - Dimens.space3,
                bottom = Dimens.space3,
            ),
        ) { _, artistAlbum ->
            AlbumCell(
                album = artistAlbum.album.cached,
                isSaved = state.savedAlbumsStates?.get(artistAlbum.albumId.value)?.value,
                showRating = true,
                ratings = state.albumRatings[artistAlbum.albumId.value]?.map { it.value },
                onToggleSave = { save ->
                    presenter.emitAsync(
                        ArtistPresenter.Event.ToggleSave(albumId = artistAlbum.albumId.value, save = save),
                    )
                },
            )
        }
    } else {
        PageLoadingSpinner()
    }
}
