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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dominiczirbel.cache.SpotifyCache
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.ui.util.RemoteState

private val IMAGE_SIZE = 400.dp
private val CELL_ROUNDING = 16.dp

@Composable
fun BoxScope.Artist(page: ArtistPage) {
    val state = RemoteState.of {
        // TODO fetch in parallel?
        Pair(
            SpotifyCache.Artists.getFullArtist(id = page.artistId),
            SpotifyCache.Artists.getArtistAlbums(artistId = page.artistId)
        )
    }

    ScrollingPage(state) { (artist, albums) ->
        Column {
            Text(artist.name, fontSize = Dimens.fontTitle)

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
                        .clickable { /* TODO */ }
                        .padding(Dimens.space3)
                ) {
                    LoadedImage(
                        url = album.images.firstOrNull()?.url,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(Modifier.height(Dimens.space2))

                    Text(
                        text = album.name,
                        fontSize = Dimens.fontBody,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.CenterHorizontally).widthIn(max = IMAGE_SIZE)
                    )
                }
            }
        }
    }
}
