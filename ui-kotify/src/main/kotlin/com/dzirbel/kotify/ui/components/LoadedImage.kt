package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.times
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.ui.SpotifyImageCache
import com.dzirbel.kotify.ui.theme.Colors
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.theme.surfaceBackground
import com.dzirbel.kotify.ui.util.instrumentation.instrument
import com.dzirbel.kotify.ui.util.roundToIntSize
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Displays an image from the lazy [image] source (only read during the drawing phase) as a square with the given [size]
 * and clipped to [shape].
 *
 * This goes to some pains (re-implementing a plain [Image] Composable or [Modifier.paint]) ensure [image] is only
 * invoked in the drawing phase, to avoid unnecessary recompositions/layouts.
 */
@Composable
fun LoadedImage(
    image: () -> ImageBitmap?,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.contentImage,
    shape: Shape = RoundedCornerShape(Dimens.cornerSize),
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
) {
    LocalColors.current.WithSurface(increment = Colors.INCREMENT_LARGE) {
        Layout(
            content = {},
            modifier = modifier
                .instrument()
                .size(size)
                .clip(shape)
                .surfaceBackground()
                .drawWithCache {
                    val imageBitmap = image()
                    if (imageBitmap == null) {
                        onDrawWithContent {}
                    } else {
                        val painter = BitmapPainter(imageBitmap)

                        val drawSize = this.size
                        val srcSize = painter.intrinsicSize
                        val scaledSize = if (drawSize.width != 0f && drawSize.height != 0f) {
                            val scaleFactor = contentScale.computeScaleFactor(srcSize = srcSize, dstSize = drawSize)
                            srcSize * scaleFactor
                        } else {
                            Size.Zero
                        }

                        val alignedPosition = alignment.align(
                            size = scaledSize.roundToIntSize(),
                            space = drawSize.roundToIntSize(),
                            layoutDirection = layoutDirection,
                        )
                        val dx = alignedPosition.x.toFloat()
                        val dy = alignedPosition.y.toFloat()

                        onDrawWithContent {
                            translate(dx, dy) {
                                with(painter) {
                                    draw(size = scaledSize)
                                }
                            }
                        }
                    }
                },
            measurePolicy = { _, constraints ->
                layout(constraints.minWidth, constraints.minHeight) {}
            },
        )
    }
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
 * Variant of [LoadedImage] which asynchronously maps urls from [urlFlow] to images loaded via the [SpotifyImageCache].
 */
@Composable
fun LoadedImage(
    urlFlow: StateFlow<String?>?,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.contentImage,
    shape: Shape = RoundedCornerShape(Dimens.cornerSize),
) {
    val initialValue = remember(urlFlow) {
        urlFlow?.value?.let { SpotifyImageCache.getFromMemory(it) }
    }

    val imageState = remember(urlFlow) {
        // use flatMapLatest to avoid reverting to a previous image if the url changes while loading
        urlFlow
            ?.filterNotNull()
            ?.flatMapLatest { SpotifyImageCache.get(it) }
    }
        ?.collectAsState(initial = initialValue)

    LoadedImage(image = { imageState?.value }, modifier = modifier, size = size, shape = shape)
}
