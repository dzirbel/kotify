package com.dzirbel.kotify.ui.page.albums

import com.dzirbel.kotify.cache.SpotifyImageCache
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.AlbumRepository
import com.dzirbel.kotify.db.model.SavedAlbumRepository
import com.dzirbel.kotify.repository.SavedRepository
import com.dzirbel.kotify.ui.components.adapter.ListAdapter
import com.dzirbel.kotify.ui.framework.Presenter
import com.dzirbel.kotify.ui.properties.AlbumNameProperty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

class AlbumsPresenter(scope: CoroutineScope) : Presenter<AlbumsPresenter.ViewModel, AlbumsPresenter.Event>(
    scope = scope,
    startingEvents = listOf(Event.Load(invalidate = false)),
    initialState = ViewModel(),
) {

    data class ViewModel(
        val refreshing: Boolean = false,
        val albums: ListAdapter<Album> = ListAdapter.empty(defaultSort = AlbumNameProperty),
        val savedAlbumIds: Set<String>? = null,
        val albumsUpdated: Long? = null,
    )

    sealed class Event {
        data class Load(val invalidate: Boolean) : Event()
        data class ReactToAlbumsSaved(val albumIds: List<String>, val saved: Boolean) : Event()
        data class ToggleSave(val albumId: String, val save: Boolean) : Event()
    }

    override fun externalEvents(): Flow<Event> {
        return merge(
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
                mutateState { it.copy(refreshing = true) }

                if (event.invalidate) {
                    SavedAlbumRepository.invalidateLibrary()
                }

                val savedAlbumIds = SavedAlbumRepository.getLibrary()
                val albums = fetchAlbums(albumIds = savedAlbumIds.toList())
                val albumsUpdated = SavedAlbumRepository.libraryUpdated()

                mutateState {
                    ViewModel(
                        refreshing = false,
                        albums = it.albums.withElements(albums),
                        savedAlbumIds = savedAlbumIds,
                        albumsUpdated = albumsUpdated?.toEpochMilli(),
                    )
                }
            }

            is Event.ReactToAlbumsSaved ->
                if (event.saved) {
                    // if an album has been saved but is now missing from the grid of albums, load and add it
                    val stateAlbums = queryState { it.albums }

                    val missingAlbumIds: List<String> = event.albumIds
                        .minus(stateAlbums.mapTo(mutableSetOf()) { it.id.value })

                    if (missingAlbumIds.isNotEmpty()) {
                        val missingAlbums = fetchAlbums(albumIds = missingAlbumIds)
                        val allAlbums = stateAlbums.plusElements(missingAlbums)

                        mutateState {
                            it.copy(albums = allAlbums, savedAlbumIds = it.savedAlbumIds?.plus(event.albumIds))
                        }
                    } else {
                        mutateState {
                            it.copy(savedAlbumIds = it.savedAlbumIds?.plus(event.albumIds))
                        }
                    }
                } else {
                    // if an album has been unsaved, retain the grid of albums but toggle its save state
                    mutateState {
                        it.copy(savedAlbumIds = it.savedAlbumIds?.minus(event.albumIds.toSet()))
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

        val imageUrls = KotifyDatabase.transaction("load albums images") {
            albums.mapNotNull { it.largestImage.live?.url }
        }
        SpotifyImageCache.loadFromFileCache(urls = imageUrls, scope = scope)

        return albums
    }
}
