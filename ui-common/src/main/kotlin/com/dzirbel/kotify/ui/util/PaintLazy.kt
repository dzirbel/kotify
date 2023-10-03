package com.dzirbel.kotify.ui.util

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.times
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/**
 * A version of [Modifier.paint] which only invokes [lazyPainter] during the drawing phase, to avoid unnecessary
 * recompositions when the painter changes (such as when an image is loaded from the network).
 */
fun Modifier.paintLazy(
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    tint: Color = Color.Unspecified,
    lazyPainter: (size: Size) -> Painter?,
): Modifier {
    return drawWithCache {
        val drawSize = this.size
        val painter = lazyPainter(drawSize)
        val colorFilter = if (tint == Color.Unspecified) null else ColorFilter.tint(tint)

        if (painter == null) {
            onDrawWithContent {}
        } else {
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
                        // TODO scaling is very poor quality
                        draw(size = scaledSize, alpha = alpha, colorFilter = colorFilter)
                    }
                }
            }
        }
    }
}

private fun Size.roundToIntSize() = IntSize(width.roundToInt(), height.roundToInt())
