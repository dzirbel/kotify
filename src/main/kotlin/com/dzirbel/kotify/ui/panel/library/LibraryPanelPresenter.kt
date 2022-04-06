package com.dzirbel.kotify.ui.panel.library

import com.dzirbel.kotify.db.model.Playlist
import com.dzirbel.kotify.db.model.PlaylistRepository
import com.dzirbel.kotify.db.model.SavedPlaylistRepository
import com.dzirbel.kotify.ui.framework.Presenter
import kotlinx.coroutines.CoroutineScope

class LibraryPanelPresenter(scope: CoroutineScope) :
    Presenter<LibraryPanelPresenter.ViewModel?, LibraryPanelPresenter.Event>(
        scope = scope,
        startingEvents = listOf(Event.LoadPlaylists()),
        initialState = null,
    ) {

    data class ViewModel(val refreshing: Boolean, val playlists: List<Playlist>, val playlistsUpdated: Long?)

    sealed class Event {
        class LoadPlaylists(val invalidate: Boolean = false) : Event()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            is Event.LoadPlaylists -> {
                mutateState { it?.copy(refreshing = true) }

                if (event.invalidate) {
                    SavedPlaylistRepository.invalidateLibrary()
                }

                val playlistIds = SavedPlaylistRepository.getLibrary().toList()
                val playlists = PlaylistRepository.get(ids = playlistIds).filterNotNull()
                    .sortedBy { it.createdTime }

                val playlistsUpdated = SavedPlaylistRepository.libraryUpdated()?.toEpochMilli()

                mutateState {
                    ViewModel(
                        refreshing = false,
                        playlists = playlists,
                        playlistsUpdated = playlistsUpdated,
                    )
                }
            }
        }
    }
}
