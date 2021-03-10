package com.dominiczirbel.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dominiczirbel.cache.SpotifyCache
import com.dominiczirbel.cache.SpotifyImageCache
import com.dominiczirbel.ui.theme.Colors
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.ui.util.RemoteState
import com.dominiczirbel.ui.util.callbackAsState

private val IMAGE_SIZE = 400.dp
private val CELL_ROUNDING = 16.dp
private val IMAGE_ROUNDING = 8.dp

@Composable
fun BoxScope.Artists() {
    val state = RemoteState.of {
        SpotifyCache.Artists.getSavedArtists()
            .map { SpotifyCache.Artists.getFullArtist(it) }
            .sortedBy { it.name }
    }

    ScrollingPage(state) { artists ->
        Column {
            Text("Artists", color = Colors.current.text, fontSize = Dimens.fontTitle)

            Spacer(Modifier.height(Dimens.space3))

            Grid(
                elements = artists,
                horizontalSpacing = Dimens.space2,
                verticalSpacing = Dimens.space3,
                verticalCellAlignment = Alignment.Top
            ) { artist ->
                Column(Modifier.clip(RoundedCornerShape(CELL_ROUNDING)).clickable { }.padding(Dimens.space3)) {
                    val imageState = artist.images.firstOrNull()?.url?.let { url ->
                        callbackAsState(key = url) { SpotifyImageCache.get(url = url) }
                    }
                    val imageBitmap = imageState?.value

                    // TODO extract to a common LoadedImage function and use a better placeholder
                    val imageModifier = Modifier
                        .size(IMAGE_SIZE)
                        .clip(RoundedCornerShape(IMAGE_ROUNDING))
                        .align(Alignment.CenterHorizontally)
                    if (imageBitmap == null) {
                        Box(imageModifier.background(Color.LightGray))
                    } else {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = "artist image",
                            modifier = imageModifier
                        )
                    }

                    Spacer(Modifier.height(Dimens.space2))

                    Text(
                        text = artist.name,
                        color = Colors.current.text,
                        fontSize = Dimens.fontBody,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.CenterHorizontally).widthIn(max = IMAGE_SIZE)
                    )
                }
            }
        }
    }
}
