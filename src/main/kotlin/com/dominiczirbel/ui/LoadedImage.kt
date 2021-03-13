package com.dominiczirbel.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dominiczirbel.cache.SpotifyImageCache
import com.dominiczirbel.ui.util.callbackAsState

private val IMAGE_SIZE = 200.dp
private val IMAGE_ROUNDING = 4.dp

@Composable
fun LoadedImage(url: String?, modifier: Modifier = Modifier, contentDescription: String? = null) {
    val imageState = url?.let {
        callbackAsState(key = url) { SpotifyImageCache.get(url = url) }
    }
    val imageBitmap = imageState?.value

    val imageModifier = modifier
        .size(IMAGE_SIZE)
        .clip(RoundedCornerShape(IMAGE_ROUNDING))

    if (imageBitmap == null) {
        Box(imageModifier.background(Color.LightGray))
    } else {
        Image(
            bitmap = imageBitmap,
            contentDescription = contentDescription,
            modifier = imageModifier
        )
    }
}
