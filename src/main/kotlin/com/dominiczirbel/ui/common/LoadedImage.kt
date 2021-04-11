package com.dominiczirbel.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.dominiczirbel.cache.SpotifyImageCache
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.ui.util.callbackAsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@Composable
fun LoadedImage(
    url: String?,
    size: Dp = Dimens.contentImage,
    modifier: Modifier = Modifier,
    scope: CoroutineScope = rememberCoroutineScope { Dispatchers.IO },
    contentDescription: String? = null
) {
    val imageState = url?.let {
        callbackAsState(key = url) { SpotifyImageCache.get(url = url, scope = scope) }
    }
    val imageBitmap = imageState?.value

    val imageModifier = modifier
        .size(size)
        .clip(RoundedCornerShape(Dimens.cornerSize))

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
