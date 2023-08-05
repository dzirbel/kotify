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
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.repository.album.AlbumRepository
import com.dzirbel.kotify.repository.album.SavedAlbumRepository
import com.dzirbel.kotify.ui.components.AlbumCell
import com.dzirbel.kotify.ui.components.LibraryInvalidateButton
import com.dzirbel.kotify.ui.components.PageLoadingSpinner
import com.dzirbel.kotify.ui.components.adapter.rememberListAdapterState
import com.dzirbel.kotify.ui.components.grid.Grid
import com.dzirbel.kotify.ui.framework.Page
import com.dzirbel.kotify.ui.framework.VerticalScrollPage
import com.dzirbel.kotify.ui.page.album.AlbumPage
import com.dzirbel.kotify.ui.pageStack
import com.dzirbel.kotify.ui.properties.AlbumNameProperty
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.derived
import com.dzirbel.kotify.ui.util.mutate
import com.dzirbel.kotify.util.combinedStateWhenAllNotNull
import com.dzirbel.kotify.util.flatMapLatestIn
import kotlinx.coroutines.flow.MutableStateFlow

object AlbumsPage : Page<Unit>() {
    @Composable
    override fun BoxScope.bind(visible: Boolean) {
        val albumsAdapter = rememberListAdapterState(defaultSort = AlbumNameProperty) { scope ->
            SavedAlbumRepository.library.flatMapLatestIn(scope) { library ->
                library?.ids
                    ?.let { albumIds ->
                        AlbumRepository.statesOf(albumIds).combinedStateWhenAllNotNull { it?.cachedValue }
                    }
                    ?: MutableStateFlow(null)
            }
        }

        VerticalScrollPage(
            visible = visible,
            onHeaderVisibilityChanged = { headerVisible -> navigationTitleState.targetState = !headerVisible },
            header = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Dimens.space4),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Albums", style = MaterialTheme.typography.h5)

                    Column {
                        LibraryInvalidateButton(savedRepository = SavedAlbumRepository)
                    }
                }
            },
            content = {
                if (albumsAdapter.derived { it.hasElements }.value) {
                    Grid(elements = albumsAdapter.value) { _, album ->
                        AlbumCell(
                            album = album,
                            onClick = { pageStack.mutate { to(AlbumPage(albumId = album.id)) } },
                        )
                    }
                } else {
                    PageLoadingSpinner()
                }
            },
        )
    }

    override fun titleFor(data: Unit) = "Saved Albums"
}
