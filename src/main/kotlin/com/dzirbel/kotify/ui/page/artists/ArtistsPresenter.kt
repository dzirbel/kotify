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
import com.dzirbel.kotify.ui.components.adapter.Divider
import com.dzirbel.kotify.ui.components.adapter.ListAdapter
import com.dzirbel.kotify.ui.components.adapter.Sort
import com.dzirbel.kotify.ui.components.adapter.SortOrder
import com.dzirbel.kotify.ui.framework.RemoteStatePresenter
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
        val artists: ListAdapter<Artist>,
        val artistsById: Map<String, Artist>,
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
        data class SetDivider(val divider: Divider<Artist>?) : Event()
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
                val artistsById = artists.associateBy { it.id.value }
                val artistsUpdated = SavedArtistRepository.libraryUpdated()

                initializeLoadedState {
                    val sorts = it?.artists?.sorts ?: listOf(Sort(SortArtistByName, SortOrder.ASCENDING))
                    val divider = it?.artists?.divider

                    ViewModel(
                        refreshing = false,
                        // TODO combine calls
                        artists = ListAdapter(artists)
                            .withDivider(divider)
                            .withSort(sorts),
                        artistsById = artistsById,
                        artistDetails = it?.artistDetails.orEmpty(),
                        savedArtistIds = savedArtistIds,
                        artistsUpdated = artistsUpdated?.toEpochMilli(),
                    )
                }
            }

            is Event.LoadArtistDetails -> {
                // don't load details again if already in the state
                if (queryState { it.viewModel?.artistDetails }?.containsKey(event.artistId) == true) return

                val artist = queryState { it.viewModel?.artistsById }?.get(event.artistId)
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
                    val stateArtists = queryState { it.viewModel?.artistsById }?.keys.orEmpty()

                    val missingArtistIds: List<String> = event.artistIds
                        .minus(stateArtists)

                    if (missingArtistIds.isNotEmpty()) {
                        val missingArtists: List<Artist> = fetchArtists(artistIds = missingArtistIds)
                        val missingArtistsById = missingArtists.associateBy { it.id.value }

                        mutateLoadedState {
                            it.copy(
                                artistsById = it.artistsById.plus(missingArtistsById),
                                artists = it.artists.plusElements(missingArtists),
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
                it.copy(artists = it.artists.withSort(event.sorts))
            }

            is Event.SetDivider -> mutateLoadedState {
                it.copy(artists = it.artists.withDivider(event.divider))
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
