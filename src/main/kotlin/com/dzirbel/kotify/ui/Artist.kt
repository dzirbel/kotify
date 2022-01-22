package com.dzirbel.kotify.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.cache.SpotifyImageCache
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.AlbumRepository
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.ArtistRepository
import com.dzirbel.kotify.db.model.SavedAlbumRepository
import com.dzirbel.kotify.ui.components.Grid
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.PageStack
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.mutate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private class ArtistPresenter(
    private val page: ArtistPage,
    private val pageStack: MutableState<PageStack>,
    scope: CoroutineScope,
) : Presenter<ArtistPresenter.ViewModel?, ArtistPresenter.Event>(
    scope = scope,
    key = page.artistId,
    eventMergeStrategy = EventMergeStrategy.LATEST,
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

@Composable
fun BoxScope.Artist(pageStack: MutableState<PageStack>, page: ArtistPage) {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val presenter = remember(page) { ArtistPresenter(page = page, pageStack = pageStack, scope = scope) }

    ScrollingPage(scrollState = pageStack.value.currentScrollState, presenter = presenter) { state ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.space4),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(state.artist.name, fontSize = Dimens.fontTitle)

            Column {
                InvalidateButton(
                    modifier = Modifier.align(Alignment.End),
                    refreshing = state.refreshingArtist,
                    updated = state.artist.updatedTime.toEpochMilli(),
                    updatedFormat = { "Artist last updated $it" },
                    updatedFallback = "Artist never updated",
                    onClick = {
                        presenter.emitAsync(
                            ArtistPresenter.Event.Load(
                                refreshArtist = true,
                                invalidateArtist = true,
                                refreshArtistAlbums = false,
                                invalidateArtistAlbums = false
                            )
                        )
                    }
                )

                InvalidateButton(
                    modifier = Modifier.align(Alignment.End),
                    refreshing = state.refreshingArtistAlbums,
                    updated = state.artist.albumsFetched?.toEpochMilli(),
                    updatedFormat = { "Albums last updated $it" },
                    updatedFallback = "Albums never updated",
                    onClick = {
                        presenter.emitAsync(
                            ArtistPresenter.Event.Load(
                                refreshArtist = false,
                                invalidateArtist = false,
                                refreshArtistAlbums = true,
                                invalidateArtistAlbums = true
                            )
                        )
                    }
                )
            }
        }

        VerticalSpacer(Dimens.space3)

        Grid(elements = state.artistAlbums) { album ->
            AlbumCell(
                album = album,
                isSaved = state.savedAlbumsState.value?.contains(album.id.value),
                pageStack = pageStack,
                onToggleSave = { save ->
                    presenter.emitAsync(ArtistPresenter.Event.ToggleSave(albumId = album.id.value, save = save))
                }
            )
        }
    }
}
