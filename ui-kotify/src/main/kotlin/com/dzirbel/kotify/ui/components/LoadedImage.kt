package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.db.model.Image
import com.dzirbel.kotify.db.model.ImageSize
import com.dzirbel.kotify.ui.SpotifyImageCache
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.KotifyColors
import com.dzirbel.kotify.ui.util.paintLazy
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull

/**
 * Converts this [Dp] to an [ImageSize] with its width and height in pixels.
 */
@Composable
fun Dp.toImageSize(): ImageSize {
    return with(LocalDensity.current) { roundToPx() }.let { ImageSize(it, it) }
}

/**
 * Displays an image from the lazy [image] source (only read during the drawing phase) as a square with the given [size]
 * and clipped to [shape].
 *
 * This goes to some pains (rather than using a plain [Image] Composable or [Modifier.paint]) to ensure [image] is only
 * invoked in the drawing phase, to avoid unnecessary recompositions/layouts.
 */
@Composable
fun LoadedImage(
    image: (size: Size) -> ImageBitmap?,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.contentImage,
    shape: Shape = RoundedCornerShape(Dimens.cornerSize),
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
) {
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(KotifyColors.current.imagePlaceholder)
            .paintLazy(alignment = alignment, contentScale = contentScale) { pixelSize ->
                image(pixelSize)?.let { BitmapPainter(it) }
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
        produceState(initialValue = SpotifyImageCache.getFromMemory(url), key1 = url) {
            SpotifyImageCache.get(url = url).firstOrNull { it != null }?.let { value = it }
        }
    }

    LoadedImage(image = { imageState?.value }, modifier = modifier, size = size, shape = shape)
}

/**
 * Variant of [LoadedImage] which maps urls from [StateFlow] produced by [urlFlowForSize] with the pixel-based
 * destination [ImageSize] to images loaded via the [SpotifyImageCache].
 */
@Composable
fun LoadedImage(
    modifier: Modifier = Modifier,
    key: Any? = null,
    size: Dp = Dimens.contentImage,
    shape: Shape = RoundedCornerShape(Dimens.cornerSize),
    urlFlowForSize: (size: ImageSize) -> StateFlow<String?>?,
) {
    val imageSize = size.toImageSize()
    val urlFlow = remember(size, key) { urlFlowForSize(imageSize) }

    val imageState = if (urlFlow == null) {
        // no need to produceState if there is no flow
        remember { mutableStateOf(null) }
    } else {
        val initialValue = remember(size, key) {
            urlFlow.value?.let { SpotifyImageCache.getFromMemory(it) }
        }

        if (initialValue == null) {
            produceState<ImageBitmap?>(initialValue = null, key1 = size, key2 = key) {
                // only collect the first non-null value from the urlFlow; it is assumed to never emit again
                urlFlow.firstOrNull { it != null }?.let { url ->
                    // only collect the first non-null value from the SpotifyImageCache; it will never emit again
                    SpotifyImageCache.get(url).firstOrNull { it != null }?.let { value = it }
                }
            }
        } else {
            // no need to produceState if we already have an image from memory
            remember(size, key) { mutableStateOf(initialValue) }
        }
    }

    LoadedImage(image = { imageState.value }, modifier = modifier, size = size, shape = shape)
}
