package com.dzirbel.kotify.ui.page.library.artists

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.ArtistAlbumTable
import com.dzirbel.kotify.db.model.ArtistRepository
import com.dzirbel.kotify.db.model.ArtistTable
import com.dzirbel.kotify.db.model.SavedArtistRepository
import com.dzirbel.kotify.ui.components.adapter.ListAdapter
import com.dzirbel.kotify.ui.components.adapter.Sort
import com.dzirbel.kotify.ui.framework.Presenter
import com.dzirbel.kotify.util.filterNotNullValues
import com.dzirbel.kotify.util.flatMapParallel
import com.dzirbel.kotify.util.zipToMap
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.update

class ArtistsLibraryStatePresenter(scope: CoroutineScope) :
    Presenter<ArtistsLibraryStatePresenter.ViewModel, ArtistsLibraryStatePresenter.Event>(
        scope = scope,
        startingEvents = listOf(Event.Load(fromCache = true)),
        initialState = ViewModel()
    ) {

    data class ViewModel(
        // ids of the saved artists
        val savedArtistIds: ListAdapter<String> = ListAdapter.empty(),

        // map from artist id to the artist model in the cache; separate from savedArtistIds since not all artist models
        // might be present in the cache
        val artists: Map<String, Artist> = emptyMap(),

        val artistsUpdated: Long? = null,

        val syncingSavedArtists: Boolean = false,

        // ids of artists currently being synced
        val syncingArtists: Set<String> = emptySet(),

        // ids of artists whose albums are currently being synced
        val syncingArtistsAlbums: Set<String> = emptySet(),
    )

    sealed class Event {
        class Load(val fromCache: Boolean) : Event()
        class RefreshArtist(val artistId: String) : Event()
        class RefreshArtistAlbums(val artistId: String) : Event()

        object FetchMissingArtists : Event()
        object InvalidateArtists : Event()
        object FetchMissingArtistAlbums : Event()
        object InvalidateArtistAlbums : Event()

        class SetSort(val sorts: List<Sort<String>>) : Event()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            is Event.Load -> {
                mutateState { it.copy(syncingSavedArtists = true) }

                val savedArtistIds = if (event.fromCache) {
                    SavedArtistRepository.getLibraryCached()?.toList()
                } else {
                    SavedArtistRepository.getLibraryRemote().toList()
                }

                val artists = loadArtists(artistIds = savedArtistIds)
                val artistsUpdated = SavedArtistRepository.libraryUpdated()?.toEpochMilli()

                mutateState {
                    ViewModel(
                        savedArtistIds = it.savedArtistIds.withElements(savedArtistIds),
                        artists = artists,
                        artistsUpdated = artistsUpdated,
                        syncingSavedArtists = false,
                    )
                }
            }

            is Event.RefreshArtist -> {
                mutateState { it.copy(syncingArtists = it.syncingArtists.plus(event.artistId)) }

                val artist = ArtistRepository.getRemote(id = event.artistId)
                    ?.also { prepArtists(listOf(it)) }

                mutateState {
                    it.copy(
                        artists = it.artists.plus(event.artistId to artist).filterNotNullValues(),
                        syncingArtists = it.syncingArtists.minus(event.artistId),
                    )
                }
            }

            is Event.RefreshArtistAlbums -> {
                mutateState { it.copy(syncingArtistsAlbums = it.syncingArtistsAlbums.plus(event.artistId)) }

                Artist.getAllAlbums(artistId = event.artistId, allowCache = false)

                val artist = ArtistRepository.getCached(id = event.artistId)
                    ?.also { prepArtists(listOf(it)) }

                mutateState {
                    it.copy(
                        artists = it.artists.plus(event.artistId to artist).filterNotNullValues(),
                        syncingArtistsAlbums = it.syncingArtistsAlbums.minus(event.artistId),
                    )
                }
            }

            Event.FetchMissingArtists -> {
                val artistIds = SavedArtistRepository.getLibraryCached()?.toList()
                val artists = loadArtists(artistIds = artistIds) { ArtistRepository.getFull(ids = it) }

                mutateState { it.copy(artists = artists) }
            }

            Event.InvalidateArtists -> {
                val artists = loadArtists(artistIds = SavedArtistRepository.getLibraryCached()?.toList()) {
                    ArtistRepository.getRemote(ids = it)
                }

                mutateState { it.copy(artists = artists) }
            }

            Event.FetchMissingArtistAlbums -> {
                val artistIds = SavedArtistRepository.getLibraryCached()?.toList()

                // TODO also fetch albums for artists not in the database at all
                loadArtists(artistIds)
                    .filterValues { artist -> !artist.hasAllAlbums }
                    .values
                    .flatMapParallel { artist ->
                        Artist.getAllAlbums(artistId = artist.id.value).second
                    }

                // reload artists from the cache
                val artists = loadArtists(artistIds = artistIds)

                mutateState { it.copy(artists = artists) }
            }

            Event.InvalidateArtistAlbums -> {
                KotifyDatabase.transaction("invalidate artists albums") {
                    ArtistAlbumTable.deleteAll()

                    ArtistTable.update(where = { Op.TRUE }) {
                        it[albumsFetched] = null
                    }
                }

                // reload artists form the cache
                val artistIds = SavedArtistRepository.getLibraryCached()?.toList()
                val artists = loadArtists(artistIds = artistIds)

                mutateState { it.copy(artists = artists) }
            }

            is Event.SetSort -> mutateState {
                it.copy(savedArtistIds = it.savedArtistIds.withSort(sorts = event.sorts))
            }
        }
    }

    private suspend fun loadArtists(
        artistIds: List<String>?,
        fetchArtists: suspend (List<String>) -> List<Artist?> = { ArtistRepository.getCached(ids = it) },
    ): Map<String, Artist> {
        return artistIds
            ?.zipToMap(fetchArtists(artistIds))
            ?.filterNotNullValues()
            ?.also { artists -> prepArtists(artists.values) }
            .orEmpty()
    }

    private suspend fun prepArtists(artists: Iterable<Artist>) {
        KotifyDatabase.transaction("load artists albums") {
            artists.forEach { artist -> artist.artistAlbums.loadToCache() }
        }
    }
}
