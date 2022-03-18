package com.dzirbel.kotify.ui.page.library.artists

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.AlbumTable
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.ArtistRepository
import com.dzirbel.kotify.db.model.ArtistTable
import com.dzirbel.kotify.db.model.SavedArtistRepository
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
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.update

class ArtistsLibraryStatePresenter(scope: CoroutineScope) :
    Presenter<ArtistsLibraryStatePresenter.ViewModel?, ArtistsLibraryStatePresenter.Event>(
        scope = scope,
        startingEvents = listOf(Event.Load),
        initialState = null
    ) {

    data class ViewModel(
        // ids of the saved artists
        val savedArtistIds: ListAdapter<String>?,

        // map from artist id to the artist model in the cache; separate from savedArtistIds since not all artist models
        // might be present in the cache
        val artists: Map<String, Artist>,

        val artistsUpdated: Long?,

        val syncingSavedArtists: Boolean = false,

        // ids of artists currently being synced
        val syncingArtists: Set<String> = emptySet(),

        // ids of artists whose albums are currently being synced
        val syncingArtistsAlbums: Set<String> = emptySet(),
    )

    sealed class Event {
        object Load : Event()

        object RefreshSavedArtists : Event()
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
            Event.Load -> {
                val savedArtistIds = SavedArtistRepository.getLibraryCached()?.toList()
                val artists = savedArtistIds
                    ?.zipToMap(ArtistRepository.getCached(ids = savedArtistIds))
                    ?.filterNotNullValues()
                    .orEmpty()
                val artistsUpdated = SavedArtistRepository.libraryUpdated()?.toEpochMilli()

                KotifyDatabase.transaction {
                    artists.values.forEach { it.albums.loadToCache() }
                }

                mutateState {
                    ViewModel(
                        savedArtistIds = savedArtistIds?.let {
                            ListAdapter.from(it, defaultSort = listOf(Sort(LibraryArtistIDColumn.sortableProperty)))
                        },
                        artists = artists,
                        artistsUpdated = artistsUpdated,
                    )
                }
            }

            Event.RefreshSavedArtists -> {
                mutateState { it?.copy(syncingSavedArtists = true) }

                SavedArtistRepository.invalidateLibrary()

                val savedArtistIds = SavedArtistRepository.getLibrary().toList()
                val artists = savedArtistIds
                    .zipToMap(ArtistRepository.getCached(ids = savedArtistIds))
                    .filterNotNullValues()
                val artistsUpdated = SavedArtistRepository.libraryUpdated()?.toEpochMilli()

                mutateState {
                    it?.copy(
                        savedArtistIds = ListAdapter.from(elements = savedArtistIds, baseAdapter = it.savedArtistIds),
                        artists = artists,
                        artistsUpdated = artistsUpdated,
                        syncingSavedArtists = false,
                    )
                }
            }

            is Event.RefreshArtist -> {
                mutateState { it?.copy(syncingArtists = it.syncingArtists.plus(event.artistId)) }

                val artist = ArtistRepository.getRemote(id = event.artistId)
                KotifyDatabase.transaction { artist?.albums?.loadToCache() }

                mutateState {
                    it?.copy(
                        artists = if (artist == null) {
                            it.artists.minus(event.artistId)
                        } else {
                            it.artists.plus(event.artistId to artist)
                        },
                        syncingArtists = it.syncingArtists.minus(event.artistId),
                    )
                }
            }

            is Event.RefreshArtistAlbums -> {
                mutateState { it?.copy(syncingArtistsAlbums = it.syncingArtistsAlbums.plus(event.artistId)) }

                ArtistRepository.getCached(id = event.artistId)?.let { artist ->
                    KotifyDatabase.transaction { artist.albumsFetched = null }
                }
                Artist.getAllAlbums(artistId = event.artistId)

                val artist = ArtistRepository.getCached(id = event.artistId)
                KotifyDatabase.transaction { artist?.albums?.loadToCache() }

                mutateState {
                    it?.copy(
                        artists = if (artist == null) {
                            it.artists.minus(event.artistId)
                        } else {
                            it.artists.plus(event.artistId to artist)
                        },
                        syncingArtistsAlbums = it.syncingArtistsAlbums.minus(event.artistId),
                    )
                }
            }

            Event.FetchMissingArtists -> {
                val artistIds = requireNotNull(SavedArtistRepository.getLibraryCached()).toList()
                val artists = artistIds
                    .zipToMap(ArtistRepository.getFull(ids = artistIds))
                    .filterNotNullValues()

                mutateState { it?.copy(artists = artists) }
            }

            Event.InvalidateArtists -> {
                val artistIds = requireNotNull(SavedArtistRepository.getLibraryCached()).toList()
                ArtistRepository.invalidate(ids = artistIds)

                val artists = artistIds
                    .zipToMap(ArtistRepository.getCached(ids = artistIds))
                    .filterNotNullValues()

                mutateState { it?.copy(artists = artists) }
            }

            Event.FetchMissingArtistAlbums -> {
                val artistIds = requireNotNull(SavedArtistRepository.getLibraryCached()).toList()
                val missingIds = KotifyDatabase.transaction {
                    Artist.find { ArtistTable.albumsFetched eq null }
                        .map { it.id.value }
                }
                    .filter { artistIds.contains(it) }

                missingIds
                    .asFlow()
                    .flatMapMerge { id ->
                        flow<Unit> { Artist.getAllAlbums(artistId = id) }
                    }
                    .collect()

                val artists = artistIds
                    .zipToMap(ArtistRepository.getCached(ids = artistIds))
                    .filterNotNullValues()

                mutateState { it?.copy(artists = artists) }
            }

            Event.InvalidateArtistAlbums -> {
                KotifyDatabase.transaction {
                    AlbumTable.AlbumArtistTable.deleteAll()

                    ArtistTable.update(where = { Op.TRUE }) {
                        it[albumsFetched] = null
                    }
                }

                val artistIds = requireNotNull(SavedArtistRepository.getLibraryCached()).toList()
                val artists = artistIds
                    .zipToMap(ArtistRepository.getCached(ids = artistIds))
                    .filterNotNullValues()

                mutateState { it?.copy(artists = artists) }
            }

            is Event.SetSort -> mutateState {
                it?.copy(savedArtistIds = it.savedArtistIds?.withSort(sorts = event.sorts))
            }
        }
    }
}
