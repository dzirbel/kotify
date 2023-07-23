package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.TransactionReadOnlyCachedProperty
import com.dzirbel.kotify.db.model.Image
import com.dzirbel.kotify.ui.SpotifyImageCache
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.instrumentation.instrument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

/**
 * Displays an image from the lazy [image] source (only read during the drawing phase) as a square with the given [size]
 * and clipped to [shape].
 */
@Composable
fun LoadedImage(
    image: () -> ImageBitmap?,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.contentImage,
    shape: Shape = RoundedCornerShape(Dimens.cornerSize),
) {
    Layout(
        content = {},
        modifier = modifier
            .instrument()
            .size(size)
            .clip(shape)
            .drawWithCache {
                val painter = image()?.let { BitmapPainter(it) }
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

/**
 * Variant of [LoadedImage] which asynchronously loads the image from the given [url] via the [SpotifyImageCache].
 */
@Composable
fun LoadedImage(
    url: String?,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.contentImage,
    shape: Shape = RoundedCornerShape(Dimens.cornerSize),
) {
    val imageState = url?.let {
        produceState(initialValue = SpotifyImageCache.getFromMemory(url)) {
            SpotifyImageCache.get(url = url).firstOrNull { it != null }?.let { value = it }
        }
    }

    LoadedImage(image = { imageState?.value }, modifier = modifier, size = size, shape = shape)
}

/**
 * Variant of [LoadedImage] which reads the image URL from the given [imageProperty] and loads the image from the
 * [SpotifyImageCache].
 */
@Composable
fun LoadedImage(
    imageProperty: TransactionReadOnlyCachedProperty<Image?>?,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.contentImage,
    shape: Shape = RoundedCornerShape(Dimens.cornerSize),
) {
    val imageState = remember(imageProperty) {
        mutableStateOf(imageProperty?.cachedOrNull?.url?.let { SpotifyImageCache.getFromMemory(url = it) })
    }

    if (imageState.value == null && imageProperty != null) {
        LaunchedEffect(imageProperty) {
            withContext(Dispatchers.Default) {
                val image = imageProperty.cachedOrNull
                    ?: KotifyDatabase.transaction(imageProperty.transactionName) { imageProperty.live }

                if (image != null) {
                    SpotifyImageCache.get(image.url)
                        .firstOrNull { it != null }
                        ?.let { imageState.value = it }
                }
            }
        }
    }

    LoadedImage(image = { imageState.value }, modifier = modifier, size = size, shape = shape)
}
