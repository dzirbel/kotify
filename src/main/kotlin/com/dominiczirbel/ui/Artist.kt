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
import androidx.compose.runtime.mutableStateOf
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
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.ui.util.RemoteState
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking

private val IMAGE_SIZE = 200.dp
private val CELL_ROUNDING = 8.dp

private data class ArtistState(
    val artist: FullArtist,
    val artistUpdated: Long?,
    val artistAlbums: List<Album>,
    val artistAlbumsUpdated: Long?
)

data class UpdateEvent(val refreshArtist: Boolean, val refreshArtistAlbums: Boolean)

@Composable
fun BoxScope.Artist(page: ArtistPage) {
    val refreshingArtist = remember { mutableStateOf(false) }
    val refreshingArtistAlbums = remember { mutableStateOf(false) }

    val sharedFlow = remember { MutableSharedFlow<UpdateEvent>() }
    val scope = rememberCoroutineScope()
    val remoteState = RemoteState.of(
        sharedFlow = sharedFlow,
        initial = UpdateEvent(
            refreshArtist = true,
            refreshArtistAlbums = true
        )
    ) { previousState: ArtistState?, event: UpdateEvent ->
        val deferredArtist = if (event.refreshArtist || previousState == null) {
            scope.async { SpotifyCache.Artists.getFullArtist(id = page.artistId) }
                .also { it.invokeOnCompletion { refreshingArtist.value = false } }
        } else {
            scope.async { previousState.artist }
        }

        val deferredArtistAlbums = if (event.refreshArtistAlbums || previousState == null) {
            scope.async { SpotifyCache.Artists.getArtistAlbums(artistId = page.artistId) }
                .also { it.invokeOnCompletion { refreshingArtistAlbums.value = false } }
        } else {
            scope.async { previousState.artistAlbums }
        }

        val artist = deferredArtist.await()
        val artistAlbums = deferredArtistAlbums.await()

        ArtistState(
            artist = artist,
            artistAlbums = artistAlbums,
            artistUpdated = SpotifyCache.lastUpdated(artist.id),
            artistAlbumsUpdated = SpotifyCache.lastUpdated(
                SpotifyCache.GlobalObjects.ArtistAlbums.idFor(artistId = artist.id)
            )
        )
    }

    ScrollingPage(remoteState) { state ->
        val artist = state.artist
        val albums = state.artistAlbums

        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(artist.name, fontSize = Dimens.fontTitle)

                Column {
                    InvalidateButton(
                        modifier = Modifier.align(Alignment.End),
                        refreshing = refreshingArtist,
                        updated = state.artistUpdated,
                        updatedFormat = { "Artist last updated $it" },
                        updatedFallback = "Artist never updated",
                        onClick = {
                            SpotifyCache.invalidate(page.artistId)
                            runBlocking {
                                sharedFlow.emit(UpdateEvent(refreshArtist = true, refreshArtistAlbums = false))
                            }
                        }
                    )

                    InvalidateButton(
                        modifier = Modifier.align(Alignment.End),
                        refreshing = refreshingArtistAlbums,
                        updated = state.artistAlbumsUpdated,
                        updatedFormat = { "Albums last updated $it" },
                        updatedFallback = "Albums never updated",
                        onClick = {
                            SpotifyCache.invalidate(
                                SpotifyCache.GlobalObjects.ArtistAlbums.idFor(artistId = page.artistId)
                            )
                            runBlocking {
                                sharedFlow.emit(UpdateEvent(refreshArtist = false, refreshArtistAlbums = true))
                            }
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
                        .clickable { /* TODO go to album page */ }
                        .padding(Dimens.space3)
                ) {
                    LoadedImage(
                        url = album.images.firstOrNull()?.url,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
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
