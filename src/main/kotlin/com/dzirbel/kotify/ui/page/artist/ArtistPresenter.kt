package com.dzirbel.kotify.ui.page.artist

import androidx.compose.runtime.State
import com.dzirbel.kotify.cache.SpotifyImageCache
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.AlbumRepository
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.ArtistRepository
import com.dzirbel.kotify.db.model.SavedAlbumRepository
import com.dzirbel.kotify.ui.framework.Presenter
import com.dzirbel.kotify.ui.pageStack
import com.dzirbel.kotify.ui.util.mutate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class ArtistPresenter(
    private val page: ArtistPage,
    scope: CoroutineScope,
) : Presenter<ArtistPresenter.ViewModel?, ArtistPresenter.Event>(
    scope = scope,
    key = page.artistId,
    startingEvents = listOf(
        Event.Load(
            refreshArtist = true,
            refreshArtistAlbums = true,
            invalidateArtist = false,
            invalidateArtistAlbums = false
        )
    ),
    initialState = null
) {

    data class ViewModel(
        val artist: Artist,
        val refreshingArtist: Boolean,
        val artistAlbums: List<Album>,
        val savedAlbumsState: State<Set<String>?>,
        val refreshingArtistAlbums: Boolean,
    )

    sealed class Event {
        data class Load(
            val refreshArtist: Boolean,
            val refreshArtistAlbums: Boolean,
            val invalidateArtist: Boolean,
            val invalidateArtistAlbums: Boolean,
        ) : Event()

        data class ToggleSave(val albumId: String, val save: Boolean) : Event()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            is Event.Load -> {
                mutateState {
                    it?.copy(
                        refreshingArtist = event.refreshArtist,
                        refreshingArtistAlbums = event.refreshArtistAlbums
                    )
                }

                if (event.invalidateArtist) {
                    AlbumRepository.invalidate(id = page.artistId)
                }

                if (event.invalidateArtistAlbums) {
                    ArtistRepository.getCached(id = page.artistId)?.let { artist ->
                        KotifyDatabase.transaction { artist.albumsFetched = null }
                    }
                }

                val artist: Artist?
                val artistAlbums: List<Album>?

                coroutineScope {
                    val deferredArtist = if (event.refreshArtist) {
                        async(Dispatchers.IO) {
                            ArtistRepository.getFull(id = page.artistId)
                        }
                    } else {
                        null
                    }

                    val deferredArtistAlbums = if (event.refreshArtistAlbums) {
                        async(Dispatchers.IO) {
                            Artist.getAllAlbums(artistId = page.artistId)
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

                artist?.let {
                    pageStack.mutate { withPageTitle(title = page.titleFor(artist)) }
                }

                artistAlbums?.let { albums ->
                    val albumUrls = KotifyDatabase.transaction {
                        albums.mapNotNull { it.largestImage.live?.url }
                    }
                    SpotifyImageCache.loadFromFileCache(urls = albumUrls, scope = scope)
                }

                val savedAlbumsState = SavedAlbumRepository.libraryState()

                mutateState {
                    ViewModel(
                        artist = artist ?: it?.artist ?: error("no artist"),
                        refreshingArtist = false,
                        artistAlbums = checkNotNull(artistAlbums ?: it?.artistAlbums),
                        savedAlbumsState = savedAlbumsState,
                        refreshingArtistAlbums = false
                    )
                }
            }

            is Event.ToggleSave -> SavedAlbumRepository.setSaved(id = event.albumId, saved = event.save)
        }
    }
}
