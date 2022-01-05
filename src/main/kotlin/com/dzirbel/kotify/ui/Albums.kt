package com.dzirbel.kotify.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.cache.SpotifyImageCache
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.AlbumRepository
import com.dzirbel.kotify.db.model.SavedAlbumRepository
import com.dzirbel.kotify.ui.components.Grid
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.PageStack
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.util.plusOrMinus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

private class AlbumsPresenter(scope: CoroutineScope) :
    Presenter<AlbumsPresenter.State?, AlbumsPresenter.Event>(
        scope = scope,
        eventMergeStrategy = EventMergeStrategy.LATEST,
        startingEvents = listOf(Event.Load(invalidate = false)),
        initialState = null
    ) {

    data class State(
        val refreshing: Boolean,
        val albums: List<Album>,
        val savedAlbumIds: Set<String>,
        val albumsUpdated: Long?
    )

    sealed class Event {
        data class Load(val invalidate: Boolean) : Event()
        data class ToggleSave(val albumId: String, val save: Boolean) : Event()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            is Event.Load -> {
                mutateState { it?.copy(refreshing = true) }

                if (event.invalidate) {
                    SavedAlbumRepository.invalidateLibrary()
                }

                val savedAlbumIds = SavedAlbumRepository.getLibrary()
                val albums = AlbumRepository.get(ids = savedAlbumIds.toList())
                    .filterNotNull()
                    .sortedBy { it.name }
                val albumsUpdated = SavedAlbumRepository.libraryUpdated()

                SpotifyImageCache.loadFromFileCache(
                    urls = albums.mapNotNull { it.images.firstOrNull()?.url },
                    scope = scope,
                )

                mutateState {
                    State(
                        refreshing = false,
                        albums = albums,
                        savedAlbumIds = savedAlbumIds,
                        albumsUpdated = albumsUpdated?.toEpochMilli()
                    )
                }
            }

            is Event.ToggleSave -> {
                SavedAlbumRepository.setSaved(id = event.albumId, saved = event.save)
                mutateState {
                    it?.copy(savedAlbumIds = it.savedAlbumIds.plusOrMinus(event.albumId, event.save))
                }
            }
        }
    }
}

@Composable
fun BoxScope.Albums(pageStack: MutableState<PageStack>) {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val presenter = remember { AlbumsPresenter(scope = scope) }

    ScrollingPage(scrollState = pageStack.value.currentScrollState, presenter = presenter) { state ->
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Albums", fontSize = Dimens.fontTitle)

                Column {
                    InvalidateButton(
                        refreshing = state.refreshing,
                        updated = state.albumsUpdated,
                        onClick = { presenter.emitAsync(AlbumsPresenter.Event.Load(invalidate = true)) }
                    )
                }
            }

            VerticalSpacer(Dimens.space3)

            Grid(
                elements = state.albums,
                horizontalSpacing = Dimens.space2,
                verticalSpacing = Dimens.space3,
                cellAlignment = Alignment.TopCenter,
            ) { album ->
                AlbumCell(
                    album = album,
                    isSaved = state.savedAlbumIds.contains(album.id.value),
                    pageStack = pageStack,
                    onToggleSave = { save ->
                        presenter.emitAsync(AlbumsPresenter.Event.ToggleSave(albumId = album.id.value, save = save))
                    }
                )
            }
        }
    }
}
