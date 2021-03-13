package com.dominiczirbel.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dominiczirbel.cache.SpotifyCache
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.ui.util.RemoteState
import com.dominiczirbel.ui.util.mutate

private val IMAGE_SIZE = 200.dp
private val CELL_ROUNDING = 8.dp

@Composable
fun BoxScope.Artists(pageStack: MutableState<PageStack>) {
    val state = RemoteState.of {
        SpotifyCache.Artists.getSavedArtists()
            .map { SpotifyCache.Artists.getFullArtist(it) }
            .sortedBy { it.name }
    }

    ScrollingPage(state) { artists ->
        Column {
            Text("Artists", fontSize = Dimens.fontTitle)

            Spacer(Modifier.height(Dimens.space3))

            Grid(
                elements = artists,
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
                        fontSize = Dimens.fontBody,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.CenterHorizontally).widthIn(max = IMAGE_SIZE)
                    )
                }
            }
        }
    }
}
