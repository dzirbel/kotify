package com.dzirbel.kotify.ui.util.instrumentation

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dzirbel.kotify.ui.theme.KotifyColors
import com.dzirbel.kotify.ui.theme.KotifyTypography
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

private val panelFontSize = 10.sp
private val panelPadding = 2.dp

/**
 * Displays a panel of metrics from the composition, layout, and drawing phases of this composable.
 *
 * @param tag an optional name displayed at the top of the panel; by default the name of the function calling this one
 */
@Stable
fun Modifier.metricsPanel(tag: String? = null): Modifier {
    return this.composed(inspectorInfo = debugInspectorInfo { name = "metricsPanel" }) {
        if (!LocalInstrumentationMetricsPanelsEnabled.current) {
            @Suppress("LabeledExpression")
            return@composed Modifier
        }

        var totalCompositions: Int by remember { Ref(0) }
        totalCompositions++

        var lastLayoutDuration: Duration? by remember { Ref(null) }
        var maxLayoutDuration: Duration? by remember { Ref(null) }
        var totalLayoutDuration: Duration by remember { Ref(Duration.ZERO) }
        var totalLayouts: Int by remember { Ref(0) }

        var maxDrawDuration: Duration? by remember { Ref(null) }
        var totalDrawDuration: Duration by remember { Ref(Duration.ZERO) }
        var totalDraws: Int by remember { Ref(0) }

        val textMeasurer = rememberTextMeasurer()
        val backgroundColor = MaterialTheme.colors.background.copy(alpha = KotifyColors.OVERLAY_ALPHA)
        val textColor = MaterialTheme.colors.onBackground

        Modifier
            .layout { measurable, constraints ->
                measureTimedValue {
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
                    .also { (_, layoutDuration) ->
                        lastLayoutDuration = layoutDuration
                        totalLayoutDuration += layoutDuration
                        maxLayoutDuration = maxLayoutDuration?.coerceAtLeast(layoutDuration) ?: layoutDuration
                        totalLayouts += 1
                    }
                    .value
            }
            .drawWithCache {
                onDrawWithContent {
                    // draw the actual content
                    val drawDuration = measureTime { drawContent() }

                    totalDrawDuration += drawDuration
                    maxDrawDuration = maxDrawDuration?.coerceAtLeast(drawDuration) ?: drawDuration
                    totalDraws += 1

                    val text = buildString {
                        if (tag != null) {
                            appendLine(tag)
                        }

                        appendLine("cmp: $totalCompositions [last | avg | max]")

                        appendLine(
                            listOf(
                                "lyt: $totalLayouts",
                                lastLayoutDuration?.formatMilliseconds(),
                                (totalLayoutDuration / totalLayouts).formatMilliseconds(),
                                maxLayoutDuration?.formatMilliseconds(),
                            ).joinToString(separator = " | "),
                        )

                        append(
                            listOf(
                                "drw: $totalDraws",
                                drawDuration.formatMilliseconds(),
                                (totalDrawDuration / totalDraws).formatMilliseconds(),
                                maxDrawDuration?.formatMilliseconds(),
                            ).joinToString(separator = " | "),
                        )
                    }

                    // measure the text with a monospace font
                    val textLayoutResult = textMeasurer.measure(
                        text = text,
                        style = TextStyle(fontSize = panelFontSize, fontFamily = KotifyTypography.Monospace),
                    )
                    val paddingPx = panelPadding.toPx()

                    val size = Size(
                        width = textLayoutResult.size.width.toFloat() + (paddingPx * 2),
                        height = textLayoutResult.size.height.toFloat() + (paddingPx * 2),
                    )

                    if (size.width <= this.size.width && size.height <= this.size.height) {
                        drawRect(brush = SolidColor(backgroundColor), size = size)

                        // draw the text
                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = Offset(paddingPx, paddingPx),
                            color = textColor,
                        )
                    }
                }
            }
    }
}

@Suppress("MagicNumber")
private fun Duration.formatMilliseconds(): String {
    val ms = this.toDouble(DurationUnit.MILLISECONDS)
    return when {
        ms < 1 -> String.format(Locale.US, "%.2fms", ms)
        ms < 10 -> String.format(Locale.US, "%.1fms", ms)
        else -> "${ms.toLong()}ms"
    }
}
