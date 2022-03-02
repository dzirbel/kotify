package com.dzirbel.kotify.ui.page.albums

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.ui.components.AlbumCell
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.components.adapter.SortOrder
import com.dzirbel.kotify.ui.components.adapter.SortableProperty
import com.dzirbel.kotify.ui.components.grid.Grid
import com.dzirbel.kotify.ui.framework.ScrollingPage
import com.dzirbel.kotify.ui.pageStack
import com.dzirbel.kotify.ui.theme.Dimens
import kotlinx.coroutines.Dispatchers

val SortAlbumsByName = object : SortableProperty<Album>(
    sortTitle = "Album Name",
    defaultOrder = SortOrder.ASCENDING,
) {
    override fun compare(first: IndexedValue<Album>, second: IndexedValue<Album>): Int {
        return first.value.name.compareTo(second.value.name)
    }
}

@Composable
fun BoxScope.Albums() {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val presenter = remember { AlbumsPresenter(scope = scope) }

    ScrollingPage(scrollState = pageStack.value.currentScrollState, presenter = presenter) { state ->
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

        VerticalSpacer(Dimens.space3)

        Grid(elements = state.albums) { _, album ->
            AlbumCell(
                album = album,
                isSaved = state.savedAlbumIds.contains(album.id.value),
                onToggleSave = { save ->
                    presenter.emitAsync(AlbumsPresenter.Event.ToggleSave(albumId = album.id.value, save = save))
                }
            )
        }
    }
}
