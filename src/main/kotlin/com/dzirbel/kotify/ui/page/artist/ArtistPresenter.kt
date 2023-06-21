package com.dzirbel.kotify.ui.page.artist

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import com.dzirbel.kotify.cache.SpotifyImageCache
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.ArtistAlbum
import com.dzirbel.kotify.db.model.ArtistRepository
import com.dzirbel.kotify.db.model.SavedAlbumRepository
import com.dzirbel.kotify.db.model.TrackRatingRepository
import com.dzirbel.kotify.network.model.SpotifyAlbum
import com.dzirbel.kotify.repository.Rating
import com.dzirbel.kotify.ui.components.adapter.AdapterProperty
import com.dzirbel.kotify.ui.components.adapter.Divider
import com.dzirbel.kotify.ui.components.adapter.ListAdapter
import com.dzirbel.kotify.ui.components.adapter.Sort
import com.dzirbel.kotify.ui.framework.Presenter
import com.dzirbel.kotify.ui.properties.AlbumNameProperty
import com.dzirbel.kotify.ui.properties.AlbumRatingProperty
import com.dzirbel.kotify.ui.properties.AlbumReleaseDateProperty
import com.dzirbel.kotify.ui.properties.AlbumTypeDividableProperty
import com.dzirbel.kotify.util.ignore
import com.dzirbel.kotify.util.zipToPersistentMap
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach

class ArtistPresenter(
    private val artistId: String,
    scope: CoroutineScope,
) : Presenter<ArtistPresenter.ViewModel, ArtistPresenter.Event>(
    scope = scope,
    key = artistId,
    startingEvents = listOf(Event.LoadArtistAlbums(invalidate = false)),
    initialState = ViewModel(),
) {

    @Stable // necessary due to use of StateFlow
    data class ViewModel(
        val artist: Artist? = null,
        val refreshingArtist: Boolean = false,
        val displayedAlbumTypes: PersistentSet<SpotifyAlbum.Type> = persistentSetOf(SpotifyAlbum.Type.ALBUM),
        val artistAlbums: ListAdapter<ArtistAlbum> = ListAdapter.empty(
            defaultSort = AlbumReleaseDateProperty.ForArtistAlbum,
            defaultFilter = filterFor(displayedAlbumTypes),
        ),
        val albumRatings: PersistentMap<String, List<State<Rating?>>?> = persistentMapOf(),
        val savedAlbumsStates: PersistentMap<String, StateFlow<Boolean?>>? = null,
        val refreshingArtistAlbums: Boolean = false,
    ) {
        val artistAlbumProperties: List<AdapterProperty<ArtistAlbum>> = listOf(
            AlbumNameProperty.ForArtistAlbum,
            AlbumReleaseDateProperty.ForArtistAlbum,
            AlbumTypeDividableProperty.ForArtistAlbum,
            AlbumRatingProperty.ForArtistAlbum(ratings = albumRatings),
        )
    }

    sealed class Event {
        object RefreshArtist : Event()

        data class LoadArtistAlbums(val invalidate: Boolean) : Event()

        class ToggleSave(val albumId: String, val save: Boolean) : Event()
        class SetSorts(val sorts: PersistentList<Sort<ArtistAlbum>>) : Event()
        class SetDivider(val divider: Divider<ArtistAlbum>?) : Event()
        class SetDisplayedAlbumTypes(val albumTypes: PersistentSet<SpotifyAlbum.Type>) : Event()
    }

    override fun externalEvents(): Flow<Event> {
        return ArtistRepository.stateOf(
            id = artistId,
            onStateInitialized = { artist ->
                if (artist == null) {
                    throw NotFound("Artist $artistId not found")
                } else {
                    mutateState { it.copy(refreshingArtist = false) }
                }
            },
        )
            .onEach { artist ->
                mutateState { it.copy(artist = artist) }
            }
            .ignore()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            is Event.RefreshArtist -> {
                mutateState { it.copy(refreshingArtist = true) }

                ArtistRepository.getRemote(id = artistId)

                mutateState { it.copy(refreshingArtist = false) }
            }

            is Event.LoadArtistAlbums -> {
                mutateState { it.copy(refreshingArtistAlbums = true) }

                val artistAlbums = ArtistRepository.getAllAlbums(artistId = artistId, allowCache = !event.invalidate)

                val albumUrls = KotifyDatabase.transaction("load artist id $artistId albums tracks and image") {
                    artistAlbums.mapNotNull { artistAlbum ->
                        artistAlbum.album.cached.trackIds.loadToCache()
                        artistAlbum.album.cached.largestImage.live?.url
                    }
                }
                SpotifyImageCache.loadFromFileCache(urls = albumUrls, scope = scope)

                val albumIds = artistAlbums.map { it.albumId.value }
                val savedAlbumsStates = albumIds.zipToPersistentMap(SavedAlbumRepository.statesOf(ids = albumIds))

                val albumRatings = artistAlbums
                    .associate { artistAlbum ->
                        val album = artistAlbum.album.cached
                        album.id.value to TrackRatingRepository.ratingStates(ids = album.trackIds.cached)
                    }
                    .toPersistentMap()

                mutateState {
                    it.copy(
                        artistAlbums = it.artistAlbums.withElements(artistAlbums),
                        albumRatings = albumRatings,
                        savedAlbumsStates = savedAlbumsStates,
                        refreshingArtistAlbums = false,
                    )
                }
            }

            is Event.ToggleSave -> SavedAlbumRepository.setSaved(id = event.albumId, saved = event.save)

            is Event.SetSorts -> mutateState {
                it.copy(artistAlbums = it.artistAlbums.withSort(event.sorts))
            }

            is Event.SetDivider -> mutateState {
                it.copy(artistAlbums = it.artistAlbums.withDivider(divider = event.divider))
            }

            is Event.SetDisplayedAlbumTypes -> mutateState {
                it.copy(
                    displayedAlbumTypes = event.albumTypes,
                    artistAlbums = it.artistAlbums.withFilter(filter = filterFor(event.albumTypes)),
                )
            }
        }
    }

    companion object {
        private fun filterFor(albumTypes: Set<SpotifyAlbum.Type>): ((ArtistAlbum) -> Boolean)? {
            return if (albumTypes.isNotEmpty()) {
                { album -> albumTypes.contains(album.albumGroup) }
            } else {
                null
            }
        }
    }
}
