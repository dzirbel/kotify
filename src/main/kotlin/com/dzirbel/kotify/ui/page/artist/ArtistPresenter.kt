package com.dzirbel.kotify.ui.page.artist

import androidx.compose.runtime.State
import com.dzirbel.kotify.cache.SpotifyImageCache
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.AlbumRepository
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.ArtistRepository
import com.dzirbel.kotify.db.model.SavedAlbumRepository
import com.dzirbel.kotify.db.model.TrackRatingRepository
import com.dzirbel.kotify.repository.Rating
import com.dzirbel.kotify.ui.components.adapter.AdapterProperty
import com.dzirbel.kotify.ui.components.adapter.Divider
import com.dzirbel.kotify.ui.components.adapter.ListAdapter
import com.dzirbel.kotify.ui.components.adapter.Sort
import com.dzirbel.kotify.ui.framework.Presenter
import com.dzirbel.kotify.ui.properties.AlbumNameProperty
import com.dzirbel.kotify.ui.properties.AlbumRatingProperty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class ArtistPresenter(
    private val artistId: String,
    scope: CoroutineScope,
) : Presenter<ArtistPresenter.ViewModel, ArtistPresenter.Event>(
    scope = scope,
    key = artistId,
    startingEvents = listOf(
        Event.Load(
            refreshArtist = true,
            refreshArtistAlbums = true,
            invalidateArtist = false,
            invalidateArtistAlbums = false
        )
    ),
    initialState = ViewModel(),
) {

    data class ViewModel(
        val artist: Artist? = null,
        val refreshingArtist: Boolean = false,
        val artistAlbums: ListAdapter<Album> = ListAdapter.empty(defaultSort = AlbumNameProperty),
        val albumRatings: Map<String, List<State<Rating?>>?> = emptyMap(),
        val savedAlbumsState: State<Set<String>?>? = null,
        val refreshingArtistAlbums: Boolean = false,
    ) {
        val artistAlbumProperties: List<AdapterProperty<Album>> = listOf(
            AlbumNameProperty,
            AlbumRatingProperty(ratings = albumRatings),
        )
    }

    sealed class Event {
        data class Load(
            val refreshArtist: Boolean,
            val refreshArtistAlbums: Boolean,
            val invalidateArtist: Boolean,
            val invalidateArtistAlbums: Boolean,
        ) : Event()

        class ToggleSave(val albumId: String, val save: Boolean) : Event()
        class SetSorts(val sorts: List<Sort<Album>>) : Event()
        class SetDivider(val divider: Divider<Album>?) : Event()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            is Event.Load -> {
                mutateState {
                    it.copy(
                        refreshingArtist = event.refreshArtist,
                        refreshingArtistAlbums = event.refreshArtistAlbums
                    )
                }

                if (event.invalidateArtist) {
                    AlbumRepository.invalidate(id = artistId)
                }

                if (event.invalidateArtistAlbums) {
                    ArtistRepository.getCached(id = artistId)?.let { artist ->
                        KotifyDatabase.transaction { artist.albumsFetched = null }
                    }
                }

                val artist: Artist?
                val artistAlbums: List<Album>?

                coroutineScope {
                    val deferredArtist = if (event.refreshArtist) {
                        async(Dispatchers.IO) {
                            ArtistRepository.getFull(id = artistId)
                        }
                    } else {
                        null
                    }

                    val deferredArtistAlbums = if (event.refreshArtistAlbums) {
                        async(Dispatchers.IO) {
                            Artist.getAllAlbums(artistId = artistId)
                        }
                    } else {
                        null
                    }

                    artist = deferredArtist?.await()
                    artistAlbums = deferredArtistAlbums?.await()
                }

                KotifyDatabase.transaction {
                    // refresh artist to get updated album fetch time
                    artist?.refresh()
                }

                artistAlbums?.let { albums ->
                    val albumUrls = KotifyDatabase.transaction {
                        albums.mapNotNull {
                            it.trackIds.loadToCache()
                            it.largestImage.live?.url
                        }
                    }
                    SpotifyImageCache.loadFromFileCache(urls = albumUrls, scope = scope)
                }

                val savedAlbumsState = SavedAlbumRepository.libraryState()

                val albumRatings = artistAlbums?.let {
                    artistAlbums.associate { album ->
                        album.id.value to album.trackIds.cached.let { TrackRatingRepository.ratingStates(ids = it) }
                    }
                }

                mutateState {
                    ViewModel(
                        artist = artist ?: it.artist ?: error("no artist"),
                        refreshingArtist = false,
                        artistAlbums = checkNotNull(
                            // apply new elements if we have them, otherwise keep existing adapter
                            artistAlbums
                                ?.let { _ -> it.artistAlbums.withElements(artistAlbums) }
                                ?: it.artistAlbums
                        ),
                        albumRatings = checkNotNull(albumRatings ?: it.albumRatings),
                        savedAlbumsState = savedAlbumsState,
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
        }
    }
}
