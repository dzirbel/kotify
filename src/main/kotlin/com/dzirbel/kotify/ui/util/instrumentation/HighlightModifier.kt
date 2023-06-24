package com.dzirbel.kotify.ui.util.instrumentation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.Settings
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val DEFAULT_RESET_TIMEOUT = 3000.milliseconds

/**
 * Displays a colored highlight border on this Composable when it is recomposed, with the color depending on how often
 * it is recomposed in the [resetTimeout] window.
 *
 * Inspired by https://www.jetpackcompose.app/articles/how-can-I-debug-recompositions-in-jetpack-compose.
 */
@Stable
fun Modifier.highlightCompositions(resetTimeout: Duration = DEFAULT_RESET_TIMEOUT): Modifier {
    return composed(inspectorInfo = debugInspectorInfo { name = "highlightCompositions" }) {
        if (!Settings.instrumentationHighlightCompositions) {
            @Suppress("LabeledExpression")
            return@composed Modifier
        }

        var totalCompositions: Long by remember { Ref(0) }
        totalCompositions++

        // use a State to trigger a draw (but not composition) on reset
        var totalCompositionsAtReset: Long by remember { mutableStateOf(0) }

        LaunchedEffect(totalCompositions) {
            delay(resetTimeout)
            totalCompositionsAtReset = totalCompositions
        }

        Modifier.drawWithCache {
            onDrawWithContent {
                drawContent() // draw real content

                val compositionsSinceReset = totalCompositions - totalCompositionsAtReset

                if (compositionsSinceReset > 0 && size.minDimension > 0) {
                    @Suppress("MagicNumber")
                    val color = when (compositionsSinceReset) {
                        1L -> Color.Blue
                        2L -> Color.Green
                        3L -> Color.Yellow
                        else -> Color.Red
                    }

                    drawRect(brush = SolidColor(color), style = Stroke(width = 2.dp.toPx()))
                }
            }
        }
    }
}
