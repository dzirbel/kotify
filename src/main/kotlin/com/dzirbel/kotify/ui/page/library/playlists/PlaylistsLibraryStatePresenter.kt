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
import com.dzirbel.kotify.util.zipToMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere

class PlaylistsLibraryStatePresenter(scope: CoroutineScope) :
    Presenter<PlaylistsLibraryStatePresenter.ViewModel?, PlaylistsLibraryStatePresenter.Event>(
        scope = scope,
        startingEvents = listOf(Event.Load),
        initialState = null
    ) {

    data class ViewModel(
        // ids of the saved playlists
        val savedPlaylistIds: ListAdapter<String>?,

        // map from playlist id to the playlist model in the cache; separate from savedPlaylistIds since not all
        // playlist models might be present in the cache
        val playlists: Map<String, Playlist>,

        val playlistsUpdated: Long?,

        val refreshingSavedPlaylists: Boolean = false,

        // ids of playlists currently being synced
        val syncingPlaylists: Set<String> = emptySet(),

        // ids of playlists whose tracks are currently being synced
        val syncingPlaylistTracks: Set<String> = emptySet(),
    )

    sealed class Event {
        object Load : Event()

        object RefreshSavedPlaylists : Event()
        class RefreshPlaylist(val playlistId: String) : Event()
        class RefreshPlaylistTracks(val playlistId: String) : Event()

        object FetchMissingPlaylists : Event()
        object InvalidatePlaylists : Event()
        object FetchMissingPlaylistTracks : Event()
        object InvalidatePlaylistTracks : Event()

        class SetSort(val sorts: List<Sort<String>>) : Event()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            Event.Load -> {
                val savedPlaylistIds = SavedPlaylistRepository.getLibraryCached()?.toList()
                val playlists = savedPlaylistIds
                    ?.zipToMap(PlaylistRepository.getCached(ids = savedPlaylistIds))
                    ?.filterNotNullValues()
                    .orEmpty()
                val playlistsUpdated = SavedPlaylistRepository.libraryUpdated()?.toEpochMilli()

                KotifyDatabase.transaction {
                    playlists.values.forEach { it.tracks.loadToCache() }
                }

                mutateState {
                    ViewModel(
                        savedPlaylistIds = savedPlaylistIds?.let { ListAdapter.from(it) },
                        playlists = playlists,
                        playlistsUpdated = playlistsUpdated,
                    )
                }
            }

            Event.RefreshSavedPlaylists -> {
                mutateState { it?.copy(refreshingSavedPlaylists = true) }

                SavedPlaylistRepository.invalidateLibrary()

                val savedPlaylistIds = SavedPlaylistRepository.getLibrary().toList()
                val playlists = savedPlaylistIds
                    .zipToMap(PlaylistRepository.getCached(ids = savedPlaylistIds))
                    .filterNotNullValues()
                val playlistsUpdated = SavedPlaylistRepository.libraryUpdated()?.toEpochMilli()

                mutateState {
                    it?.copy(
                        savedPlaylistIds = ListAdapter.from(
                            elements = savedPlaylistIds,
                            baseAdapter = it.savedPlaylistIds,
                        ),
                        playlists = playlists,
                        playlistsUpdated = playlistsUpdated,
                        refreshingSavedPlaylists = false,
                    )
                }
            }

            is Event.RefreshPlaylist -> {
                mutateState { it?.copy(syncingPlaylists = it.syncingPlaylists.plus(event.playlistId)) }

                val playlist = PlaylistRepository.getRemote(id = event.playlistId)
                KotifyDatabase.transaction { playlist?.tracks?.loadToCache() }

                mutateState {
                    it?.copy(
                        playlists = if (playlist == null) {
                            it.playlists.minus(event.playlistId)
                        } else {
                            it.playlists.plus(event.playlistId to playlist)
                        },
                        syncingPlaylists = it.syncingPlaylists.minus(event.playlistId),
                    )
                }
            }

            is Event.RefreshPlaylistTracks -> {
                mutateState { it?.copy(syncingPlaylistTracks = it.syncingPlaylistTracks.plus(event.playlistId)) }

                KotifyDatabase.transaction {
                    PlaylistTrackTable.deleteWhere { PlaylistTrackTable.playlist eq event.playlistId }
                }

                PlaylistRepository.getCached(id = event.playlistId)?.getAllTracks()

                val playlist = PlaylistRepository.getCached(id = event.playlistId)
                KotifyDatabase.transaction { playlist?.tracks?.loadToCache() }

                mutateState {
                    it?.copy(
                        playlists = if (playlist == null) {
                            it.playlists.minus(event.playlistId)
                        } else {
                            it.playlists.plus(event.playlistId to playlist)
                        },
                        syncingPlaylistTracks = it.syncingPlaylistTracks.minus(event.playlistId),
                    )
                }
            }

            Event.FetchMissingPlaylists -> {
                val playlistIds = requireNotNull(SavedPlaylistRepository.getLibraryCached()).toList()
                val playlists = playlistIds
                    .zipToMap(PlaylistRepository.getFull(ids = playlistIds))
                    .filterNotNullValues()

                mutateState { it?.copy(playlists = playlists) }
            }

            Event.InvalidatePlaylists -> {
                val playlistIds = requireNotNull(SavedPlaylistRepository.getLibraryCached()).toList()
                PlaylistRepository.invalidate(ids = playlistIds)

                val playlists = playlistIds
                    .zipToMap(PlaylistRepository.getCached(ids = playlistIds))
                    .filterNotNullValues()

                mutateState { it?.copy(playlists = playlists) }
            }

            Event.FetchMissingPlaylistTracks -> {
                val playlistIds = requireNotNull(SavedPlaylistRepository.getLibraryCached()).toList()
                val playlists = PlaylistRepository.get(ids = playlistIds)
                KotifyDatabase.transaction {
                    playlists.onEach { it?.tracks?.loadToCache() }
                }

                // TODO also fetch tracks for playlists not in the database at all
                val missingTracks = KotifyDatabase.transaction {
                    playlists.filter { it?.hasAllTracks == false }
                }

                missingTracks
                    .asFlow()
                    .flatMapMerge { playlist ->
                        flow<Unit> { playlist?.getAllTracks() }
                    }
                    .collect()

                val playlists2 = playlistIds
                    .zipToMap(PlaylistRepository.getCached(ids = playlistIds))
                    .filterNotNullValues()
                KotifyDatabase.transaction { playlists2.values.forEach { it.tracks.loadToCache() } }

                mutateState { it?.copy(playlists = playlists2) }
            }

            Event.InvalidatePlaylistTracks -> {
                KotifyDatabase.transaction { PlaylistTrackTable.deleteAll() }

                val playlistIds = requireNotNull(SavedPlaylistRepository.getLibraryCached()).toList()
                val playlists = playlistIds
                    .zipToMap(PlaylistRepository.getCached(ids = playlistIds))
                    .filterNotNullValues()
                mutateState { it?.copy(playlists = playlists) }
            }

            is Event.SetSort -> mutateState {
                it?.copy(savedPlaylistIds = it.savedPlaylistIds?.withSort(sorts = event.sorts))
            }
        }
    }
}
