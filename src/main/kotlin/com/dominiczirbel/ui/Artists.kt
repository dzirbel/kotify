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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dominiczirbel.cache.SpotifyCache
import com.dominiczirbel.network.model.FullArtist
import com.dominiczirbel.ui.common.Grid
import com.dominiczirbel.ui.common.InvalidateButton
import com.dominiczirbel.ui.common.LoadedImage
import com.dominiczirbel.ui.common.PageStack
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.ui.util.RemoteState
import com.dominiczirbel.ui.util.mutate
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking

private val IMAGE_SIZE = 200.dp
private val CELL_ROUNDING = 8.dp

private data class ArtistsState(
    val artists: List<FullArtist>,
    val artistsUpdated: Long?
)

@Composable
fun BoxScope.Artists(pageStack: MutableState<PageStack>) {
    val refreshing = remember { mutableStateOf(false) }
    val sharedFlow = remember { MutableSharedFlow<Unit>() }
    val state = RemoteState.of(sharedFlow = sharedFlow) {
        val artists = SpotifyCache.Artists.getSavedArtists()
            .map { SpotifyCache.Artists.getFullArtist(it) }
            .sortedBy { it.name }

        refreshing.value = false

        ArtistsState(
            artists = artists,
            artistsUpdated = SpotifyCache.lastUpdated(SpotifyCache.GlobalObjects.SavedArtists.ID)
        )
    }

    ScrollingPage(state) { artistsState ->
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Artists", fontSize = Dimens.fontTitle)

                Column {
                    InvalidateButton(
                        refreshing = refreshing,
                        updated = artistsState.artistsUpdated,
                        onClick = {
                            SpotifyCache.invalidate(SpotifyCache.GlobalObjects.SavedArtists.ID)
                            runBlocking { sharedFlow.emit(Unit) }
                        }
                    )
                }
            }

            Spacer(Modifier.height(Dimens.space3))

            Grid(
                elements = artistsState.artists,
                horizontalSpacing = Dimens.space2,
                verticalSpacing = Dimens.space3,
                verticalCellAlignment = Alignment.Top
            ) { artist ->
                Column(
                    Modifier
                        .clip(RoundedCornerShape(CELL_ROUNDING))
                        .clickable { pageStack.mutate { to(ArtistPage(artistId = artist.id)) } }
                        .padding(Dimens.space3)
                ) {
                    LoadedImage(
                        url = artist.images.firstOrNull()?.url,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(Modifier.height(Dimens.space2))

                    Text(
                        text = artist.name,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.CenterHorizontally).widthIn(max = IMAGE_SIZE)
                    )
                }
            }
        }
    }
}
