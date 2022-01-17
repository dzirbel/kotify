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
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.AlbumRepository
import com.dzirbel.kotify.db.model.SavedAlbumRepository
import com.dzirbel.kotify.repository.SavedRepository
import com.dzirbel.kotify.ui.components.Grid
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.PageStack
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.util.plusSorted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map

private class AlbumsPresenter(scope: CoroutineScope) :
    Presenter<AlbumsPresenter.ViewModel?, AlbumsPresenter.Event>(
        scope = scope,
        eventMergeStrategy = EventMergeStrategy.LATEST,
        startingEvents = listOf(Event.Load(invalidate = false)),
        initialState = null
    ) {

    data class ViewModel(
        val refreshing: Boolean,
        val albums: List<Album>,
        val savedAlbumIds: Set<String>,
        val albumsUpdated: Long?
    )

    sealed class Event {
        data class Load(val invalidate: Boolean) : Event()
        data class ReactToAlbumsSaved(val albumIds: List<String>, val saved: Boolean) : Event()
        data class ToggleSave(val albumId: String, val save: Boolean) : Event()
    }

    override fun eventFlows(): Iterable<Flow<Event>> {
        return listOf(
            SavedAlbumRepository.eventsFlow()
                .filterIsInstance<SavedRepository.Event.SetSaved>()
                .map { Event.ReactToAlbumsSaved(albumIds = it.ids, saved = it.saved) },

            SavedAlbumRepository.eventsFlow()
                .filterIsInstance<SavedRepository.Event.QueryLibraryRemote>()
                .map { Event.Load(invalidate = false) },
        )
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            is Event.Load -> {
                mutateState { it?.copy(refreshing = true) }

                if (event.invalidate) {
                    SavedAlbumRepository.invalidateLibrary()
                }

                val savedAlbumIds = SavedAlbumRepository.getLibrary()
                val albums = fetchAlbums(albumIds = savedAlbumIds.toList())
                    .sortedBy { it.name }
                val albumsUpdated = SavedAlbumRepository.libraryUpdated()

                mutateState {
                    ViewModel(
                        refreshing = false,
                        albums = albums,
                        savedAlbumIds = savedAlbumIds,
                        albumsUpdated = albumsUpdated?.toEpochMilli(),
                    )
                }
            }

            is Event.ReactToAlbumsSaved -> {
                if (event.saved) {
                    // if an album has been saved but is now missing from the grid of albums, load and add it
                    val stateAlbums = queryState { it?.albums }.orEmpty()

                    val missingAlbumIds: List<String> = event.albumIds
                        .minus(stateAlbums.mapTo(mutableSetOf()) { it.id.value })

                    if (missingAlbumIds.isNotEmpty()) {
                        val missingAlbums = fetchAlbums(albumIds = missingAlbumIds)
                        val allAlbums = stateAlbums.plusSorted(missingAlbums) { it.name }

                        mutateState {
                            it?.copy(albums = allAlbums, savedAlbumIds = it.savedAlbumIds.plus(event.albumIds))
                        }
                    } else {
                        mutateState {
                            it?.copy(savedAlbumIds = it.savedAlbumIds.plus(event.albumIds))
                        }
                    }
                } else {
                    // if an album has been unsaved, retain the grid of albums but toggle its save state
                    mutateState {
                        it?.copy(savedAlbumIds = it.savedAlbumIds.minus(event.albumIds.toSet()))
                    }
                }
            }

            is Event.ToggleSave -> SavedAlbumRepository.setSaved(id = event.albumId, saved = event.save)
        }
    }

    /**
     * Loads the full [Album] objects from the [AlbumRepository] and does common initialization - caching their images
     * from the database and warming the image cache.
     */
    private suspend fun fetchAlbums(albumIds: List<String>): List<Album> {
        val albums = AlbumRepository.getFull(ids = albumIds).filterNotNull()

        val imageUrls = KotifyDatabase.transaction {
            albums.mapNotNull { it.images.live.firstOrNull()?.url }
        }
        SpotifyImageCache.loadFromFileCache(urls = imageUrls, scope = scope)

        return albums
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
