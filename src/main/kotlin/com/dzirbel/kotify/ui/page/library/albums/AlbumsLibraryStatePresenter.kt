package com.dzirbel.kotify.ui.page.library.albums

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.AlbumRepository
import com.dzirbel.kotify.db.model.SavedAlbumRepository
import com.dzirbel.kotify.ui.components.adapter.ListAdapter
import com.dzirbel.kotify.ui.components.adapter.Sort
import com.dzirbel.kotify.ui.framework.Presenter
import com.dzirbel.kotify.util.filterNotNullValues
import com.dzirbel.kotify.util.zipToMap
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.CoroutineScope

class AlbumsLibraryStatePresenter(scope: CoroutineScope) :
    Presenter<AlbumsLibraryStatePresenter.ViewModel, AlbumsLibraryStatePresenter.Event>(
        scope = scope,
        startingEvents = listOf(Event.Load(fromCache = true)),
        initialState = ViewModel(),
    ) {

    data class ViewModel(
        // ids of the saved albums
        val savedAlbumIds: ListAdapter<String> = ListAdapter.empty(),

        // map from album id to the album model in the cache; separate from savedAlbumsIds since not all album models
        // might be present in the cache
        val albums: Map<String, Album> = emptyMap(),

        val albumsUpdated: Long? = null,

        val syncingSavedAlbums: Boolean = false,

        // ids of albums currently being synced
        val syncingAlbums: Set<String> = emptySet(),
    )

    sealed class Event {
        class Load(val fromCache: Boolean) : Event()
        class RefreshAlbum(val albumId: String) : Event()

        object FetchMissingAlbums : Event()
        object InvalidateAlbums : Event()

        class SetSort(val sorts: PersistentList<Sort<String>>) : Event()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            is Event.Load -> {
                mutateState { it.copy(syncingSavedAlbums = true) }

                val savedAlbumIds = if (event.fromCache) {
                    SavedAlbumRepository.getLibraryCached()?.toList()
                } else {
                    SavedAlbumRepository.getLibraryRemote().toList()
                }

                val albums = loadAlbums(albumIds = savedAlbumIds)
                val albumsUpdated = SavedAlbumRepository.libraryUpdated()?.toEpochMilli()

                mutateState {
                    ViewModel(
                        savedAlbumIds = it.savedAlbumIds.withElements(savedAlbumIds),
                        albums = albums,
                        albumsUpdated = albumsUpdated,
                        syncingSavedAlbums = false,
                    )
                }
            }

            is Event.RefreshAlbum -> {
                mutateState { it.copy(syncingAlbums = it.syncingAlbums.plus(event.albumId)) }

                val album = AlbumRepository.getRemote(id = event.albumId)
                    ?.also { prepAlbums(listOf(it)) }

                mutateState {
                    it.copy(
                        albums = it.albums.plus(event.albumId to album).filterNotNullValues(),
                        syncingAlbums = it.syncingAlbums.minus(event.albumId),
                    )
                }
            }

            Event.FetchMissingAlbums -> {
                val albumIds = requireNotNull(SavedAlbumRepository.getLibraryCached()).toList()
                val albums = loadAlbums(albumIds = albumIds) { AlbumRepository.getFull(ids = it) }

                mutateState { it.copy(albums = albums) }
            }

            Event.InvalidateAlbums -> {
                val albums = loadAlbums(albumIds = SavedAlbumRepository.getLibraryCached()?.toList()) {
                    AlbumRepository.getRemote(ids = it)
                }

                mutateState { it.copy(albums = albums) }
            }

            is Event.SetSort -> mutateState {
                it.copy(savedAlbumIds = it.savedAlbumIds.withSort(sorts = event.sorts))
            }
        }
    }

    private suspend fun loadAlbums(
        albumIds: List<String>?,
        fetchAlbums: suspend (List<String>) -> List<Album?> = { AlbumRepository.getCached(ids = it) },
    ): Map<String, Album> {
        return albumIds
            ?.zipToMap(fetchAlbums(albumIds))
            ?.filterNotNullValues()
            ?.also { albums -> prepAlbums(albums.values) }
            .orEmpty()
    }

    private suspend fun prepAlbums(albums: Iterable<Album>) {
        KotifyDatabase.transaction("load albums artists") {
            albums.forEach { album -> album.artists.loadToCache() }
        }
    }
}
