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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import com.dominiczirbel.ui.common.LoadedImage
import com.dominiczirbel.ui.common.SimpleTextButton
import com.dominiczirbel.ui.common.liveRelativeDateText
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.ui.util.RemoteState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking

private val IMAGE_SIZE = 200.dp
private val CELL_ROUNDING = 8.dp

private data class State(
    val artist: FullArtist,
    val artistAlbums: List<Album>,
    val lastUpdated: Long?
)

private suspend fun fetch(scope: CoroutineScope, artistId: String): State {
    val artist = scope.async { SpotifyCache.Artists.getFullArtist(id = artistId) }
    val artistAlbums = scope.async { SpotifyCache.Artists.getArtistAlbums(artistId = artistId) }

    return State(
        artist = artist.await(),
        artistAlbums = artistAlbums.await(),

        // TODO could be different for the artist albums
        lastUpdated = SpotifyCache.lastUpdated(id = artistId)
    )
}

@Composable
fun BoxScope.Artist(page: ArtistPage) {
    val refreshing = remember { mutableStateOf(false) }

    val sharedFlow = remember { MutableSharedFlow<Unit>() }
    val scope = rememberCoroutineScope()
    val remoteState = RemoteState.of(sharedFlow = sharedFlow) {
        fetch(scope = scope, artistId = page.artistId).also {
            refreshing.value = false
        }
    }

    ScrollingPage(remoteState) { state ->
        val artist = state.artist
        val albums = state.artistAlbums

        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(artist.name, fontSize = Dimens.fontTitle)

                SimpleTextButton(
                    enabled = !refreshing.value,
                    onClick = {
                        SpotifyCache.invalidate(id = page.artistId)
                        runBlocking { sharedFlow.emit(Unit) }
                        refreshing.value = true
                    }
                ) {
                    Text(
                        text = state.lastUpdated?.let { lastUpdated ->
                            liveRelativeDateText(timestamp = lastUpdated, format = { "Last updated $it" })
                        } ?: "Never updated"
                    )

                    Spacer(Modifier.width(Dimens.space2))

                    if (refreshing.value) {
                        CircularProgressIndicator(Modifier.size(Dimens.iconMedium))
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(Dimens.iconMedium)
                        )
                    }
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
