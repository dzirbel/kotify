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
import kotlinx.coroutines.CoroutineScope

class AlbumsLibraryStatePresenter(scope: CoroutineScope) :
    Presenter<AlbumsLibraryStatePresenter.ViewModel?, AlbumsLibraryStatePresenter.Event>(
        scope = scope,
        startingEvents = listOf(Event.Load),
        initialState = null
    ) {

    data class ViewModel(
        // ids of the saved albums
        val savedAlbumIds: ListAdapter<String>?,

        // map from album id to the album model in the cache; separate from savedAlbumsIds since not all album models
        // might be present in the cache
        val albums: Map<String, Album>,

        val albumsUpdated: Long?,

        val refreshingSavedAlbums: Boolean = false,
        val syncingAlbums: Set<String> = emptySet(),
    )

    sealed class Event {
        object Load : Event()

        object RefreshSavedAlbums : Event()
        class RefreshAlbum(val albumId: String) : Event()

        object FetchMissingAlbums : Event()
        object InvalidateAlbums : Event()

        class SetSort(val sorts: List<Sort<String>>) : Event()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            Event.Load -> {
                val savedAlbumIds = SavedAlbumRepository.getLibraryCached()?.toList()
                val albums = savedAlbumIds
                    ?.zipToMap(AlbumRepository.getCached(ids = savedAlbumIds))
                    ?.filterNotNullValues()
                    .orEmpty()
                val albumsUpdated = SavedAlbumRepository.libraryUpdated()?.toEpochMilli()

                KotifyDatabase.transaction {
                    albums.values.forEach { it.artists.loadToCache() }
                }

                mutateState {
                    ViewModel(
                        savedAlbumIds = savedAlbumIds?.let {
                            ListAdapter.from(it)
                        },
                        albums = albums,
                        albumsUpdated = albumsUpdated,
                    )
                }
            }

            Event.RefreshSavedAlbums -> {
                mutateState { it?.copy(refreshingSavedAlbums = true) }

                SavedAlbumRepository.invalidateLibrary()

                val savedAlbumIds = SavedAlbumRepository.getLibrary().toList()
                val albums = savedAlbumIds
                    .zipToMap(AlbumRepository.getCached(ids = savedAlbumIds))
                    .filterNotNullValues()
                val albumsUpdated = SavedAlbumRepository.libraryUpdated()?.toEpochMilli()

                mutateState {
                    it?.copy(
                        savedAlbumIds = ListAdapter.from(elements = savedAlbumIds, baseAdapter = it.savedAlbumIds),
                        albums = albums,
                        albumsUpdated = albumsUpdated,
                        refreshingSavedAlbums = false,
                    )
                }
            }

            is Event.RefreshAlbum -> {
                mutateState { it?.copy(syncingAlbums = it.syncingAlbums.plus(event.albumId)) }

                val album = AlbumRepository.getRemote(id = event.albumId)
                KotifyDatabase.transaction { album?.artists?.loadToCache() }

                mutateState {
                    it?.copy(
                        albums = if (album == null) {
                            it.albums.minus(event.albumId)
                        } else {
                            it.albums.plus(event.albumId to album)
                        },
                        syncingAlbums = it.syncingAlbums.minus(event.albumId),
                    )
                }
            }

            Event.FetchMissingAlbums -> {
                val albumIds = requireNotNull(SavedAlbumRepository.getLibraryCached()).toList()
                val albums = albumIds
                    .zipToMap(AlbumRepository.getFull(ids = albumIds))
                    .filterNotNullValues()

                mutateState { it?.copy(albums = albums) }
            }

            Event.InvalidateAlbums -> {
                val albumIds = requireNotNull(SavedAlbumRepository.getLibraryCached()).toList()
                AlbumRepository.invalidate(ids = albumIds)

                val albums = albumIds
                    .zipToMap(AlbumRepository.getCached(ids = albumIds))
                    .filterNotNullValues()

                mutateState { it?.copy(albums = albums) }
            }

            is Event.SetSort -> mutateState {
                it?.copy(savedAlbumIds = it.savedAlbumIds?.withSort(sorts = event.sorts))
            }
        }
    }
}
