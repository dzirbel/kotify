package com.dzirbel.kotify.ui.page.artists

import androidx.compose.runtime.State
import com.dzirbel.kotify.cache.SpotifyImageCache
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.ArtistAlbum
import com.dzirbel.kotify.db.model.ArtistRepository
import com.dzirbel.kotify.db.model.SavedAlbumRepository
import com.dzirbel.kotify.db.model.SavedArtistRepository
import com.dzirbel.kotify.db.model.TrackRatingRepository
import com.dzirbel.kotify.repository.Rating
import com.dzirbel.kotify.repository.SavedRepository
import com.dzirbel.kotify.ui.components.adapter.AdapterProperty
import com.dzirbel.kotify.ui.components.adapter.Divider
import com.dzirbel.kotify.ui.components.adapter.ListAdapter
import com.dzirbel.kotify.ui.components.adapter.Sort
import com.dzirbel.kotify.ui.framework.Presenter
import com.dzirbel.kotify.ui.properties.AlbumNameProperty
import com.dzirbel.kotify.ui.properties.ArtistNameProperty
import com.dzirbel.kotify.ui.properties.ArtistPopularityProperty
import com.dzirbel.kotify.ui.properties.ArtistRatingProperty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import java.time.Instant

class ArtistsPresenter(scope: CoroutineScope) : Presenter<ArtistsPresenter.ViewModel, ArtistsPresenter.Event>(
    scope = scope,
    startingEvents = listOf(Event.Load(invalidate = false)),
    initialState = ViewModel(),
) {

    data class ArtistDetails(
        val savedTime: Instant?,
        val genres: List<String>,
        val albums: ListAdapter<ArtistAlbum>?,
    )

    data class ViewModel(
        /**
         * [ListAdapter] wrapping the saved [Artist] models to be displayed.
         */
        val artists: ListAdapter<Artist> = ListAdapter.empty(defaultSort = ArtistNameProperty),

        /**
         * Whether [artists] is being reloaded.
         */
        val refreshing: Boolean = false,

        /**
         * Map from artist ID to the live [Rating]s for all of their tracks.
         */
        val artistRatings: Map<String, List<State<Rating?>>?> = emptyMap(),

        /**
         * Map from artist ID to [ArtistDetails] which are loaded individually for each artist on demand (i.e. when the
         * grid insert is displayed).
         */
        val artistDetails: Map<String, ArtistDetails> = emptyMap(),

        /**
         * Index of the selected artist whose details are shown as an insert in the grid.
         */
        val selectedArtistIndex: Int? = null,

        /**
         * Set of artist IDs which are currently in the user's library. May not always match [artists] exactly since the
         * user could remove an artist, but it will still be displayed.
         */
        val savedArtistIds: Set<String>? = null,

        /**
         * Live set of saved album IDs.
         */
        val savedAlbumsState: State<Set<String>?>? = null,

        /**
         * Time when artists were last updated, or null if either this hasn't been loaded yet or the library has never
         * been fetched.
         */
        val artistsUpdated: Long? = null,
    ) {
        val artistProperties: List<AdapterProperty<Artist>> = listOf(
            ArtistNameProperty,
            ArtistPopularityProperty,
            ArtistRatingProperty(ratings = artistRatings),
        )
    }

    sealed class Event {
        data class Load(val invalidate: Boolean) : Event()
        data class SetSelectedArtistIndex(val index: Int?) : Event()
        data class ReactToArtistsSaved(val artistIds: List<String>, val saved: Boolean) : Event()
        data class ToggleSave(val artistId: String, val save: Boolean) : Event()
        data class ToggleAlbumSaved(val albumId: String, val save: Boolean) : Event()
        data class SetSorts(val sorts: List<Sort<Artist>>) : Event()
        data class SetDivider(val divider: Divider<Artist>?) : Event()
    }

    override fun externalEvents(): Flow<Event> {
        return merge(
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
                mutateState { it.copy(refreshing = true) }

                if (event.invalidate) {
                    SavedArtistRepository.invalidateLibrary()
                }

                val savedArtistIds = SavedArtistRepository.getLibrary()
                val artists = fetchArtists(artistIds = savedArtistIds.toList())
                val artistsUpdated = SavedArtistRepository.libraryUpdated()

                val artistRatings = artists.associate { artist ->
                    artist.id.value to TrackRatingRepository.ratingStates(ids = artist.trackIds.cached)
                }

                mutateState {
                    val selectedArtistId = it.selectedArtistIndex?.let { index -> it.artists[index]?.id?.value }

                    it.copy(
                        refreshing = false,
                        artists = it.artists.withElements(artists),
                        selectedArtistIndex = selectedArtistId
                            ?.let { artists.indexOfFirst { artist -> artist.id.value == selectedArtistId } }
                            ?.takeIf { index -> index >= 0 },
                        artistRatings = artistRatings,
                        savedArtistIds = savedArtistIds,
                        artistsUpdated = artistsUpdated?.toEpochMilli(),
                    )
                }
            }

            is Event.SetSelectedArtistIndex -> {
                mutateState { it.copy(selectedArtistIndex = event.index) }

                // do not load details the selection has been cleared
                if (event.index == null) return

                val artist = requireNotNull(queryState { it.artists[event.index] })

                // don't load details again if already in the state
                if (queryState { it.artistDetails }.containsKey(artist.id.value)) return

                val savedTime = SavedArtistRepository.savedTimeCached(id = artist.id.value)
                val genres = KotifyDatabase.transaction("load artist ${artist.name} genres") { artist.genres.live }
                    .map { it.name }
                    .sorted()

                val details = ArtistDetails(
                    savedTime = savedTime,
                    genres = genres,
                    albums = null,
                )

                mutateState {
                    it.copy(artistDetails = it.artistDetails.plus(artist.id.value to details))
                }

                val albums = ArtistRepository.getAllAlbums(artistId = artist.id.value)
                val albumsAdapter = ListAdapter.empty(AlbumNameProperty.ForArtistAlbum).withElements(albums)
                KotifyDatabase.transaction("load artist ${artist.name} albums images") {
                    albums.forEach { it.album.cached.largestImage.loadToCache() }
                }

                val savedAlbumsState = if (queryState { it.savedAlbumsState } == null) {
                    SavedAlbumRepository.libraryState()
                } else {
                    null
                }

                mutateState {
                    it.copy(
                        artistDetails = it.artistDetails.plus(artist.id.value to details.copy(albums = albumsAdapter)),
                        savedAlbumsState = savedAlbumsState ?: it.savedAlbumsState,
                    )
                }
            }

            is Event.ReactToArtistsSaved -> {
                if (event.saved) {
                    // if an artist has been saved but is now missing from the grid of artists, load and add it
                    val stateArtists = queryState { it.artists }.mapTo(mutableSetOf()) { it.id.value }

                    val missingArtistIds: List<String> = event.artistIds
                        .minus(stateArtists)

                    if (missingArtistIds.isNotEmpty()) {
                        val missingArtists: List<Artist> = fetchArtists(artistIds = missingArtistIds)
                        val missingArtistRatings = missingArtists.associate { artist ->
                            artist.id.value to TrackRatingRepository.ratingStates(ids = artist.trackIds.cached)
                        }

                        mutateState {
                            val artistRatings = it.artistRatings.plus(missingArtistRatings)
                            it.copy(
                                artists = it.artists.plusElements(missingArtists),
                                savedArtistIds = it.savedArtistIds?.plus(event.artistIds),
                                artistRatings = artistRatings,
                            )
                        }
                    } else {
                        mutateState {
                            it.copy(savedArtistIds = it.savedArtistIds?.plus(event.artistIds))
                        }
                    }
                } else {
                    // if an artist has been unsaved, retain the grid of artists but toggle its save state
                    mutateState {
                        it.copy(savedArtistIds = it.savedArtistIds?.minus(event.artistIds.toSet()))
                    }
                }
            }

            is Event.ToggleSave -> SavedArtistRepository.setSaved(id = event.artistId, saved = event.save)

            is Event.ToggleAlbumSaved -> SavedAlbumRepository.setSaved(id = event.albumId, saved = event.save)

            is Event.SetSorts -> mutateState {
                it.copy(artists = it.artists.withSort(event.sorts))
            }

            is Event.SetDivider -> mutateState {
                it.copy(artists = it.artists.withDivider(divider = event.divider))
            }
        }
    }

    /**
     * Loads the full [Artist] objects from the [ArtistRepository] and does common initialization - caching their images
     * from the database and warming the image cache.
     */
    private suspend fun fetchArtists(artistIds: List<String>): List<Artist> {
        val artists = ArtistRepository.getFull(ids = artistIds).filterNotNull()

        val imageUrls = KotifyDatabase.transaction("load artists tracks and image") {
            artists.mapNotNull { artist ->
                artist.trackIds.loadToCache()
                artist.largestImage.live?.url
            }
        }
        SpotifyImageCache.loadFromFileCache(urls = imageUrls, scope = scope)

        return artists
    }
}
