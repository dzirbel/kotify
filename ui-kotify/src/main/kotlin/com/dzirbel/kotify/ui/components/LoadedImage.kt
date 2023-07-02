package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.ui.SpotifyImageCache
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.instrumentation.instrument
import kotlinx.coroutines.Dispatchers

/**
 * Asynchronously loads an image from [url] via the [SpotifyImageCache] and displays it as a square image with the given
 * [size] and clipped to [shape].
 */
@Composable
fun LoadedImage(
    url: String?,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.contentImage,
    shape: Shape = RoundedCornerShape(Dimens.cornerSize),
) {
    val imageState = remember { mutableStateOf<ImageBitmap?>(null) }

    if (url != null) {
        val scope = rememberCoroutineScope { Dispatchers.IO }
        LaunchedEffect(url) {
            imageState.value = SpotifyImageCache.get(url = url, scope = scope)
        }
    }

    Layout(
        content = {},
        modifier = modifier
            .instrument()
            .size(size)
            .clip(shape)
            .drawWithCache {
                val painter = imageState.value?.let { BitmapPainter(it) }
                onDrawWithContent {
                    val contentSize = this.size
                    if (painter == null) {
                        drawRect(color = Color.LightGray, topLeft = Offset.Zero, size = contentSize)
                    } else {
                        with(painter) { draw(contentSize) }
                    }
                }
            },
        measurePolicy = { _, constraints ->
            layout(constraints.minWidth, constraints.minHeight) {}
        },
    )
}
