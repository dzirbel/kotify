package com.dzirbel.kotify.ui.page.albums

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.ui.components.AlbumCell
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.grid.Grid
import com.dzirbel.kotify.ui.framework.PageLoadingSpinner
import com.dzirbel.kotify.ui.theme.Dimens

@Composable
fun AlbumsPageHeader(presenter: AlbumsPresenter, state: AlbumsPresenter.ViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(Dimens.space4),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Albums", style = MaterialTheme.typography.h5)

        Column {
            InvalidateButton(
                refreshing = state.refreshing,
                updated = state.albumsUpdated,
                onClick = { presenter.emitAsync(AlbumsPresenter.Event.Load(invalidate = true)) }
            )
        }
    }
}

@Composable
fun AlbumsPageContent(presenter: AlbumsPresenter, state: AlbumsPresenter.ViewModel) {
    if (state.albums.hasElements) {
        Grid(elements = state.albums) { _, album ->
            AlbumCell(
                album = album,
                isSaved = state.savedAlbumIds?.contains(album.id.value),
                onToggleSave = { save ->
                    presenter.emitAsync(AlbumsPresenter.Event.ToggleSave(albumId = album.id.value, save = save))
                }
            )
        }
    } else {
        PageLoadingSpinner()
    }
}
