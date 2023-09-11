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

// TODO customize primary and second colors
@Suppress("MagicNumber")
enum class KotifyColors(
    private val materialColors: Colors,
    val star: Color = Color.Yellow,
    val success: Color = Color.Green,
    val warning: Color = Color.Yellow,
    val divider: Color = materialColors.onBackground.copy(alpha = 0.12f),
    val imagePlaceholder: Color = materialColors.onBackground.copy(alpha = 0.20f),
) {
    LIGHT(lightColors()),
    DARK(
        materialColors = darkColors(
            background = Color(0xFF212121),
            surface = Color(0xFF212121),
        ),
    ),
    HIGH_CONTRAST(darkColors(background = Color.Black, surface = Color.Black)),
    ;

    @Composable
    fun Apply(content: @Composable () -> Unit) {
        CompositionLocalProvider(LocalKotifyColors provides this) {
            MaterialTheme(colors = materialColors, content = content)
        }
    }

    companion object {
        const val OVERLAY_ALPHA = 0.5f

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
