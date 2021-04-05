package com.dominiczirbel.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dominiczirbel.cache.SpotifyCache
import com.dominiczirbel.network.model.Album
import com.dominiczirbel.network.model.FullArtist
import com.dominiczirbel.ui.common.Grid
import com.dominiczirbel.ui.common.InvalidateButton
import com.dominiczirbel.ui.common.LoadedImage
import com.dominiczirbel.ui.common.PageStack
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.ui.util.mutate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

private val IMAGE_SIZE = 200.dp
private val CELL_ROUNDING = 8.dp

private class ArtistPresenter(private val artistId: String, scope: CoroutineScope) :
    Presenter<ArtistPresenter.State?, ArtistPresenter.Event>(
        scope = scope,
        key = artistId,
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
        val refreshingArtistAlbums: Boolean
    )

    sealed class Event {
        data class Load(
            val refreshArtist: Boolean,
            val refreshArtistAlbums: Boolean,
            val invalidateArtist: Boolean,
            val invalidateArtistAlbums: Boolean
        ) : Event()
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
                    SpotifyCache.invalidate(id = artistId)
                }

                if (event.invalidateArtistAlbums) {
                    SpotifyCache.invalidate(
                        SpotifyCache.GlobalObjects.ArtistAlbums.idFor(artistId = artistId)
                    )
                }

                val deferredArtist = if (event.refreshArtist) {
                    scope.async(Dispatchers.IO) {
                        SpotifyCache.Artists.getFullArtist(id = artistId)
                    }
                } else {
                    null
                }

                val deferredArtistAlbums = if (event.refreshArtistAlbums) {
                    scope.async(Dispatchers.IO) {
                        SpotifyCache.Artists.getArtistAlbums(artistId = artistId, scope = scope)
                    }
                } else {
                    null
                }

                val artist = deferredArtist?.await()
                val artistAlbums = deferredArtistAlbums?.await()

                mutateState {
                    State(
                        artist = artist ?: it?.artist ?: error(""),
                        artistUpdated = artist?.id?.let { SpotifyCache.lastUpdated(it) },
                        refreshingArtist = false,
                        artistAlbums = checkNotNull(artistAlbums ?: it?.artistAlbums),
                        artistAlbumsUpdated = SpotifyCache.lastUpdated(
                            SpotifyCache.GlobalObjects.ArtistAlbums.idFor(artistId = artistId)
                        ),
                        refreshingArtistAlbums = false
                    )
                }
            }
        }
    }
}

@Composable
fun BoxScope.Artist(pageStack: MutableState<PageStack>, page: ArtistPage) {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val presenter = remember(page) { ArtistPresenter(artistId = page.artistId, scope = scope) }

    ScrollingPage(state = { presenter.state() }) { state ->
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
                Column(
                    Modifier
                        .clip(RoundedCornerShape(CELL_ROUNDING))
                        .clickable { pageStack.mutate { to(AlbumPage(albumId = album.id!!)) } }
                        .padding(Dimens.space3)
                ) {
                    LoadedImage(
                        url = album.images.firstOrNull()?.url,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        scope = scope
                    )

                    Spacer(Modifier.height(Dimens.space2))

                    Text(
                        text = album.name,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.CenterHorizontally).widthIn(max = IMAGE_SIZE)
                    )
                }
            }
        }
    }
}
