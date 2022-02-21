package com.dzirbel.kotify.ui.page.artist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.ui.components.AlbumCell
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.components.grid.Grid
import com.dzirbel.kotify.ui.framework.ScrollingPage
import com.dzirbel.kotify.ui.pageStack
import com.dzirbel.kotify.ui.theme.Dimens
import kotlinx.coroutines.Dispatchers

@Composable
fun BoxScope.Artist(page: ArtistPage) {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val presenter = remember(page) { ArtistPresenter(page = page, scope = scope) }

    ScrollingPage(scrollState = pageStack.value.currentScrollState, presenter = presenter) { state ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.space4),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(state.artist.name, fontSize = Dimens.fontTitle)

            Column {
                InvalidateButton(
                    modifier = Modifier.align(Alignment.End),
                    refreshing = state.refreshingArtist,
                    updated = state.artist.updatedTime.toEpochMilli(),
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
                    updated = state.artist.albumsFetched?.toEpochMilli(),
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

        VerticalSpacer(Dimens.space3)

        Grid(elements = state.artistAlbums) { album ->
            AlbumCell(
                album = album,
                isSaved = state.savedAlbumsState.value?.contains(album.id.value),
                onToggleSave = { save ->
                    presenter.emitAsync(ArtistPresenter.Event.ToggleSave(albumId = album.id.value, save = save))
                }
            )
        }
    }
}
