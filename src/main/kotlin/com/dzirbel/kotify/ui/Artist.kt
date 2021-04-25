package com.dzirbel.kotify.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.cache.LibraryCache
import com.dzirbel.kotify.cache.SpotifyCache
import com.dzirbel.kotify.network.model.Album
import com.dzirbel.kotify.network.model.FullArtist
import com.dzirbel.kotify.ui.common.Grid
import com.dzirbel.kotify.ui.common.InvalidateButton
import com.dzirbel.kotify.ui.common.PageStack
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.mutate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private class ArtistPresenter(
    private val page: ArtistPage,
    private val pageStack: MutableState<PageStack>,
    scope: CoroutineScope
) : Presenter<ArtistPresenter.State?, ArtistPresenter.Event>(
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

    data class State(
        val artist: FullArtist,
        val artistUpdated: Long?,
        val refreshingArtist: Boolean,
        val artistAlbums: List<Album>,
        val artistAlbumsUpdated: Long?,
        val savedAlbums: Set<String>?,
        val refreshingArtistAlbums: Boolean
    )

    sealed class Event {
        data class Load(
            val refreshArtist: Boolean,
            val refreshArtistAlbums: Boolean,
            val invalidateArtist: Boolean,
            val invalidateArtistAlbums: Boolean
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
                    SpotifyCache.invalidate(id = page.artistId)
                }

                if (event.invalidateArtistAlbums) {
                    SpotifyCache.invalidate(
                        SpotifyCache.GlobalObjects.ArtistAlbums.idFor(artistId = page.artistId)
                    )
                }

                val artist: FullArtist?
                val artistAlbums: List<Album>?

                coroutineScope {
                    val deferredArtist = if (event.refreshArtist) {
                        async(Dispatchers.IO) {
                            SpotifyCache.Artists.getFullArtist(id = page.artistId)
                        }
                    } else {
                        null
                    }

                    val deferredArtistAlbums = if (event.refreshArtistAlbums) {
                        async(Dispatchers.IO) {
                            SpotifyCache.Artists.getArtistAlbums(artistId = page.artistId)
                        }
                    } else {
                        null
                    }

                    artist = deferredArtist?.await()
                    artistAlbums = deferredArtistAlbums?.await()
                }

                artist?.let {
                    pageStack.mutate { withPageTitle(title = page.titleFor(artist)) }
                }

                val savedAlbums = LibraryCache.savedAlbums

                mutateState {
                    State(
                        artist = artist ?: it?.artist ?: error("no artist"),
                        artistUpdated = SpotifyCache.lastUpdated(page.artistId),
                        refreshingArtist = false,
                        artistAlbums = checkNotNull(artistAlbums ?: it?.artistAlbums),
                        artistAlbumsUpdated = SpotifyCache.lastUpdated(
                            SpotifyCache.GlobalObjects.ArtistAlbums.idFor(artistId = page.artistId)
                        ),
                        savedAlbums = savedAlbums,
                        refreshingArtistAlbums = false
                    )
                }
            }

            is Event.ToggleSave -> {
                val savedAlbums = if (event.save) {
                    SpotifyCache.Albums.saveAlbum(id = event.albumId)
                } else {
                    SpotifyCache.Albums.unsaveAlbum(id = event.albumId)
                }

                savedAlbums?.let {
                    mutateState { it?.copy(savedAlbums = savedAlbums) }
                }
            }
        }
    }
}

@Composable
fun BoxScope.Artist(pageStack: MutableState<PageStack>, page: ArtistPage) {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val presenter = remember(page) { ArtistPresenter(page = page, pageStack = pageStack, scope = scope) }

    ScrollingPage(scrollState = pageStack.value.currentScrollState, state = { presenter.state() }) { state ->
        val artist = state.artist
        val albums = state.artistAlbums

        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(artist.name, fontSize = Dimens.fontTitle)

                Column {
                    InvalidateButton(
                        modifier = Modifier.align(Alignment.End),
                        refreshing = state.refreshingArtist,
                        updated = state.artistUpdated,
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
                        updated = state.artistAlbumsUpdated,
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

            Spacer(Modifier.height(Dimens.space3))

            Grid(
                elements = albums,
                horizontalSpacing = Dimens.space2,
                verticalSpacing = Dimens.space3,
                verticalCellAlignment = Alignment.Top
            ) { album ->
                AlbumCell(
                    album = album,
                    savedAlbums = state.savedAlbums,
                    pageStack = pageStack,
                    onToggleSave = { save ->
                        album.id?.let { albumId ->
                            presenter.emitAsync(ArtistPresenter.Event.ToggleSave(albumId = albumId, save = save))
                        }
                    }
                )
            }
        }
    }
}
