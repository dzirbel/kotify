package com.dzirbel.kotify.ui.page.artists

import androidx.compose.runtime.State
import com.dzirbel.kotify.cache.SpotifyImageCache
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.ArtistRepository
import com.dzirbel.kotify.db.model.SavedAlbumRepository
import com.dzirbel.kotify.db.model.SavedArtistRepository
import com.dzirbel.kotify.repository.SavedRepository
import com.dzirbel.kotify.ui.components.grid.GridDivider
import com.dzirbel.kotify.ui.components.grid.GridElements
import com.dzirbel.kotify.ui.components.sort.Sort
import com.dzirbel.kotify.ui.components.sort.SortOrder
import com.dzirbel.kotify.ui.components.sort.sortedBy
import com.dzirbel.kotify.ui.framework.RemoteStatePresenter
import com.dzirbel.kotify.util.plusSorted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import java.time.Instant

class ArtistsPresenter(scope: CoroutineScope) :
    RemoteStatePresenter<ArtistsPresenter.ViewModel, ArtistsPresenter.Event>(
        scope = scope,
        startingEvents = listOf(Event.Load(invalidate = false)),
    ) {

    data class ArtistDetails(
        val savedTime: Instant?,
        val genres: List<String>,
        val albums: List<Album>?,
    )

    data class ViewModel(
        val refreshing: Boolean,
        val sorts: List<Sort<Artist>>,
        val artists: GridElements<Artist>,
        val divider: GridDivider<Artist>?,
        val artistDetails: Map<String, ArtistDetails>,
        val savedArtistIds: Set<String>,
        val savedAlbumsState: State<Set<String>?>? = null,
        val artistsUpdated: Long?,
    )

    sealed class Event {
        data class Load(val invalidate: Boolean) : Event()
        data class LoadArtistDetails(val artistId: String) : Event()
        data class ReactToArtistsSaved(val artistIds: List<String>, val saved: Boolean) : Event()
        data class ToggleSave(val artistId: String, val save: Boolean) : Event()
        data class ToggleAlbumSaved(val albumId: String, val save: Boolean) : Event()
        data class SetSorts(val sorts: List<Sort<Artist>>) : Event()
        data class SetDivider(val divider: GridDivider<Artist>?) : Event()
    }

    override fun eventFlows(): Iterable<Flow<Event>> {
        return listOf(
            SavedArtistRepository.eventsFlow()
                .filterIsInstance<SavedRepository.Event.SetSaved>()
                .map { Event.ReactToArtistsSaved(artistIds = it.ids, saved = it.saved) },

            SavedArtistRepository.eventsFlow()
                .filterIsInstance<SavedRepository.Event.QueryLibraryRemote>()
                .map { Event.Load(invalidate = false) },
        )
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            is Event.Load -> {
                mutateLoadedState { it.copy(refreshing = true) }

                if (event.invalidate) {
                    SavedArtistRepository.invalidateLibrary()
                }

                val savedArtistIds = SavedArtistRepository.getLibrary()
                val artists = fetchArtists(artistIds = savedArtistIds.toList())
                val artistsUpdated = SavedArtistRepository.libraryUpdated()

                initializeLoadedState {
                    val sorts = it?.sorts ?: listOf(Sort(SortArtistByName, SortOrder.ASCENDING))
                    val divider = it?.divider

                    ViewModel(
                        refreshing = false,
                        sorts = sorts,
                        artists = if (divider == null) {
                            GridElements.PlainList(artists.sortedBy(sorts))
                        } else {
                            GridElements.DividedList.fromList(
                                elements = artists,
                                divider = divider,
                                sorts = sorts,
                            )
                        },
                        divider = divider,
                        artistDetails = it?.artistDetails.orEmpty(),
                        savedArtistIds = savedArtistIds,
                        artistsUpdated = artistsUpdated?.toEpochMilli(),
                    )
                }
            }

            is Event.LoadArtistDetails -> {
                // don't load details again if already in the state
                if (queryState { it.viewModel?.artistDetails }?.containsKey(event.artistId) == true) return

                val artist = queryState { it.viewModel?.artists }?.elements?.find { it.id.value == event.artistId }
                    ?: ArtistRepository.getCached(id = event.artistId)
                requireNotNull(artist) { "could not resolve artist for ${event.artistId}" }

                val savedTime = SavedArtistRepository.savedTimeCached(id = event.artistId)
                val genres = KotifyDatabase.transaction { artist.genres.live }
                    .map { it.name }
                    .sorted()

                val details = ArtistDetails(
                    savedTime = savedTime,
                    genres = genres,
                    albums = null
                )

                mutateLoadedState {
                    it.copy(artistDetails = it.artistDetails.plus(event.artistId to details))
                }

                val albums = Artist.getAllAlbums(artistId = event.artistId)
                KotifyDatabase.transaction {
                    albums.forEach { it.largestImage.loadToCache() }
                }

                val savedAlbumsState = if (queryState { it.viewModel?.savedAlbumsState } == null) {
                    SavedAlbumRepository.libraryState()
                } else {
                    null
                }

                mutateLoadedState {
                    it.copy(
                        artistDetails = it.artistDetails.plus(event.artistId to details.copy(albums = albums)),
                        savedAlbumsState = savedAlbumsState ?: it.savedAlbumsState,
                    )
                }
            }

            is Event.ReactToArtistsSaved -> {
                if (event.saved) {
                    // if an artist has been saved but is now missing from the grid of artists, load and add it
                    val stateArtists = queryState { it.viewModel?.artists }?.elements.orEmpty()

                    val missingArtistIds: List<String> = event.artistIds
                        .minus(stateArtists.mapTo(mutableSetOf()) { it.id.value })

                    if (missingArtistIds.isNotEmpty()) {
                        val missingArtists = fetchArtists(artistIds = missingArtistIds)
                        val allArtists = stateArtists.plusSorted(missingArtists) { it.name }

                        mutateLoadedState {
                            it.copy(
                                artists = it.artists.withElements(allArtists),
                                savedArtistIds = it.savedArtistIds.plus(event.artistIds),
                            )
                        }
                    } else {
                        mutateLoadedState {
                            it.copy(savedArtistIds = it.savedArtistIds.plus(event.artistIds))
                        }
                    }
                } else {
                    // if an artist has been unsaved, retain the grid of artists but toggle its save state
                    mutateLoadedState {
                        it.copy(savedArtistIds = it.savedArtistIds.minus(event.artistIds.toSet()))
                    }
                }
            }

            is Event.ToggleSave -> SavedArtistRepository.setSaved(id = event.artistId, saved = event.save)

            is Event.ToggleAlbumSaved -> SavedAlbumRepository.setSaved(id = event.albumId, saved = event.save)

            is Event.SetSorts -> mutateLoadedState {
                it.copy(
                    sorts = event.sorts,
                    artists = it.artists.sortedBy(event.sorts)
                )
            }

            is Event.SetDivider -> mutateLoadedState {
                it.copy(
                    divider = event.divider,
                    artists = it.artists.withDivider(event.divider)
                )
            }
        }
    }

    /**
     * Loads the full [Artist] objects from the [ArtistRepository] and does common initialization - caching their images
     * from the database and warming the image cache.
     */
    private suspend fun fetchArtists(artistIds: List<String>): List<Artist> {
        val artists = ArtistRepository.getFull(ids = artistIds).filterNotNull()

        val imageUrls = KotifyDatabase.transaction {
            artists.mapNotNull { artist ->
                artist.largestImage.live?.url
            }
        }
        SpotifyImageCache.loadFromFileCache(urls = imageUrls, scope = scope)

        return artists
    }
}
