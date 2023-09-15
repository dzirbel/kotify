package com.dzirbel.kotify.ui.theme

import androidx.compose.material.Colors
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

private val BLUE_300 = Color(91, 155, 238)
private val BLUE_500 = Color(10, 105, 230)
private val BLUE_700 = Color(7, 75, 163)
private val STAR = Color(242, 210, 68)

data class StarColors(
    val foreground: Color,
    val background: Color,
    val outline: Color?,
    val addingAlpha: Float,
    val removingAlpha: Float,
)

@Suppress("MagicNumber")
enum class KotifyColors(
    private val materialColors: Colors,
    val star: StarColors,
    val success: Color = Color(10, 163, 33),
    val warning: Color = Color(255, 203, 107),
    val divider: Color = materialColors.onBackground.copy(alpha = 0.12f),
    val imagePlaceholder: Color = materialColors.onBackground.copy(alpha = 0.20f),
    val selectedAlpha: Float,
    val overlayAlpha: Float,
) {
    LIGHT(
        materialColors = lightColors(
            primary = BLUE_500,
            primaryVariant = BLUE_700,
            secondary = BLUE_300,
            secondaryVariant = BLUE_300,
        ),
        star = StarColors(
            foreground = STAR,
            background = Color(160, 160, 160),
            outline = Color(230, 167, 25),
            addingAlpha = 0.35f,
            removingAlpha = 0.33f,
        ),
        selectedAlpha = 0.325f,
        overlayAlpha = 0.5f,
    ),
    DARK(
        materialColors = darkColors(
            background = Color(33, 33, 33),
            surface = Color(33, 33, 33),
            primary = BLUE_500,
            primaryVariant = BLUE_300,
            secondary = BLUE_700,
            secondaryVariant = BLUE_700,
        ),
        star = StarColors(
            foreground = STAR,
            background = Color(128, 128, 128),
            outline = null,
            addingAlpha = 0.6f,
            removingAlpha = 0.4f,
        ),
        selectedAlpha = 0.35f,
        overlayAlpha = 0.5f,
    ),
    ;

    val selectedBackground: Color
        get() = materialColors.primary.copy(alpha = selectedAlpha)

    @Composable
    fun Apply(content: @Composable () -> Unit) {
        CompositionLocalProvider(LocalKotifyColors provides this) {
            MaterialTheme(colors = materialColors, content = content)
        }
    }

    companion object {
        val current: KotifyColors
            @Composable
            get() = LocalKotifyColors.current

        /**
         * Gets a highlight-aware color, i.e. [Colors.primary] if [highlight] is true and [otherwise] if not.
         *
         * Also applies the alpha from [LocalContentAlpha]. TODO should it?
         */
        @Composable
        fun highlighted(highlight: Boolean, otherwise: Color = LocalContentColor.current): Color {
            return (if (highlight) MaterialTheme.colors.primary else otherwise).let {
                it.copy(alpha = minOf(LocalContentAlpha.current, it.alpha))
            }
        }
    }
}

private val LocalKotifyColors = compositionLocalOf { KotifyColors.DARK }
