package com.dzirbel.kotify.ui.page.library.playlists

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.Playlist
import com.dzirbel.kotify.db.model.PlaylistRepository
import com.dzirbel.kotify.db.model.PlaylistTrackTable
import com.dzirbel.kotify.db.model.SavedPlaylistRepository
import com.dzirbel.kotify.ui.components.adapter.ListAdapter
import com.dzirbel.kotify.ui.components.adapter.Sort
import com.dzirbel.kotify.ui.framework.Presenter
import com.dzirbel.kotify.util.filterNotNullValues
import com.dzirbel.kotify.util.flatMapParallel
import com.dzirbel.kotify.util.zipToMap
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.exposed.sql.deleteAll

class PlaylistsLibraryStatePresenter(scope: CoroutineScope) :
    Presenter<PlaylistsLibraryStatePresenter.ViewModel, PlaylistsLibraryStatePresenter.Event>(
        scope = scope,
        startingEvents = listOf(Event.Load(fromCache = true)),
        initialState = ViewModel(),
    ) {

    data class ViewModel(
        // ids of the saved playlists
        val savedPlaylistIds: ListAdapter<String> = ListAdapter.empty(),

        // map from playlist id to the playlist model in the cache; separate from savedPlaylistIds since not all
        // playlist models might be present in the cache
        val playlists: Map<String, Playlist> = emptyMap(),

        val playlistsUpdated: Long? = null,

        val syncingSavedPlaylists: Boolean = false,

        // ids of playlists currently being synced
        val syncingPlaylists: Set<String> = emptySet(),

        // ids of playlists whose tracks are currently being synced
        val syncingPlaylistTracks: Set<String> = emptySet(),
    )

    sealed class Event {
        class Load(val fromCache: Boolean) : Event()
        class RefreshPlaylist(val playlistId: String) : Event()
        class RefreshPlaylistTracks(val playlistId: String) : Event()

        object FetchMissingPlaylists : Event()
        object InvalidatePlaylists : Event()
        object FetchMissingPlaylistTracks : Event()
        object InvalidatePlaylistTracks : Event()

        class SetSort(val sorts: PersistentList<Sort<String>>) : Event()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            is Event.Load -> {
                mutateState { it.copy(syncingSavedPlaylists = true) }

                val savedPlaylistIds = if (event.fromCache) {
                    SavedPlaylistRepository.getLibraryCached()?.toList()
                } else {
                    SavedPlaylistRepository.getLibraryRemote().toList()
                }

                val playlists = loadPlaylists(playlistIds = savedPlaylistIds)
                val playlistsUpdated = SavedPlaylistRepository.libraryUpdated()?.toEpochMilli()

                mutateState {
                    ViewModel(
                        savedPlaylistIds = it.savedPlaylistIds.withElements(savedPlaylistIds),
                        playlists = playlists,
                        playlistsUpdated = playlistsUpdated,
                        syncingSavedPlaylists = false,
                    )
                }
            }

            is Event.RefreshPlaylist -> {
                mutateState { it.copy(syncingPlaylists = it.syncingPlaylists.plus(event.playlistId)) }

                val playlist = PlaylistRepository.getRemote(id = event.playlistId)
                    ?.also { prepPlaylists(listOf(it)) }

                mutateState {
                    it.copy(
                        playlists = it.playlists.plus(event.playlistId to playlist).filterNotNullValues(),
                        syncingPlaylists = it.syncingPlaylists.minus(event.playlistId),
                    )
                }
            }

            is Event.RefreshPlaylistTracks -> {
                mutateState { it.copy(syncingPlaylistTracks = it.syncingPlaylistTracks.plus(event.playlistId)) }

                val (playlist, _) = Playlist.getAllTracks(playlistId = event.playlistId, allowCache = false)
                playlist?.let { prepPlaylists(listOf(it)) } // reload playlist from the cache

                mutateState {
                    it.copy(
                        playlists = it.playlists.plus(event.playlistId to playlist).filterNotNullValues(),
                        syncingPlaylistTracks = it.syncingPlaylistTracks.minus(event.playlistId),
                    )
                }
            }

            Event.FetchMissingPlaylists -> {
                val playlistIds = SavedPlaylistRepository.getLibraryCached()?.toList()
                val playlists = loadPlaylists(playlistIds = playlistIds) { PlaylistRepository.getFull(ids = it) }

                mutateState { it.copy(playlists = playlists) }
            }

            Event.InvalidatePlaylists -> {
                val playlists = loadPlaylists(playlistIds = SavedPlaylistRepository.getLibraryCached()?.toList()) {
                    PlaylistRepository.getRemote(ids = it)
                }

                mutateState { it.copy(playlists = playlists) }
            }

            Event.FetchMissingPlaylistTracks -> {
                val playlistIds = SavedPlaylistRepository.getLibraryCached()?.toList()

                playlistIds?.flatMapParallel { playlistId ->
                    Playlist.getAllTracks(playlistId = playlistId, allowCache = true).second
                }

                // reload playlists from the cache
                val playlists = loadPlaylists(playlistIds = playlistIds)

                mutateState { it.copy(playlists = playlists) }
            }

            Event.InvalidatePlaylistTracks -> {
                KotifyDatabase.transaction("invalidate playlists tracks") { PlaylistTrackTable.deleteAll() }

                // reload playlists from the cache
                val playlistIds = SavedPlaylistRepository.getLibraryCached()?.toList()
                val playlists = loadPlaylists(playlistIds = playlistIds)

                mutateState { it.copy(playlists = playlists) }
            }

            is Event.SetSort -> mutateState {
                it.copy(savedPlaylistIds = it.savedPlaylistIds.withSort(sorts = event.sorts))
            }
        }
    }

    private suspend fun loadPlaylists(
        playlistIds: List<String>?,
        fetchPlaylists: suspend (List<String>) -> List<Playlist?> = { PlaylistRepository.getCached(ids = it) },
    ): Map<String, Playlist> {
        return playlistIds
            ?.zipToMap(fetchPlaylists(playlistIds))
            ?.filterNotNullValues()
            ?.also { playlists -> prepPlaylists(playlists.values) }
            .orEmpty()
    }

    private suspend fun prepPlaylists(playlists: Iterable<Playlist>) {
        KotifyDatabase.transaction("load playlists tracks") {
            playlists.forEach { playlist -> playlist.tracks.loadToCache() }
        }
    }
}
