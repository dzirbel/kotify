package com.dzirbel.kotify.ui.page.artist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.ui.components.AlbumCell
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.grid.Grid
import com.dzirbel.kotify.ui.framework.PageLoadingSpinner
import com.dzirbel.kotify.ui.theme.Dimens

@Composable
fun ArtistPageHeader(presenter: ArtistPresenter, state: ArtistPresenter.ViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(Dimens.space4),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(state.artist?.name.orEmpty(), style = MaterialTheme.typography.h5)

        Column {
            InvalidateButton(
                modifier = Modifier.align(Alignment.End),
                refreshing = state.refreshingArtist,
                updated = state.artist?.updatedTime?.toEpochMilli(),
                updatedFormat = { "Artist synced $it" },
                updatedFallback = "Artist never synced",
                onClick = {
                    presenter.emitAsync(
                        ArtistPresenter.Event.Load(
                            refreshArtist = true,
                            invalidateArtist = true,
                            refreshArtistAlbums = false,
                            invalidateArtistAlbums = false
                        )
                    )
                }
            )

            InvalidateButton(
                modifier = Modifier.align(Alignment.End),
                refreshing = state.refreshingArtistAlbums,
                updated = state.artist?.albumsFetched?.toEpochMilli(),
                updatedFormat = { "Albums synced $it" },
                updatedFallback = "Albums never synced",
                onClick = {
                    presenter.emitAsync(
                        ArtistPresenter.Event.Load(
                            refreshArtist = false,
                            invalidateArtist = false,
                            refreshArtistAlbums = true,
                            invalidateArtistAlbums = true
                        )
                    )
                }
            )
        }
    }
}

@Composable
fun ArtistPageContent(presenter: ArtistPresenter, state: ArtistPresenter.ViewModel) {
    if (state.artistAlbums.hasElements) {
        Grid(elements = state.artistAlbums) { _, album ->
            AlbumCell(
                album = album,
                isSaved = state.savedAlbumsState?.value?.contains(album.id.value),
                showRating = true,
                ratings = state.albumRatings[album.id.value]?.map { it.value },
                onToggleSave = { save ->
                    presenter.emitAsync(ArtistPresenter.Event.ToggleSave(albumId = album.id.value, save = save))
                }
            )
        }
    } else {
        PageLoadingSpinner()
    }
}
