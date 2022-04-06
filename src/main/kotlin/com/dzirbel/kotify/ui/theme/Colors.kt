package com.dzirbel.kotify.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import com.dzirbel.kotify.Settings

val LocalColors: ProvidableCompositionLocal<Colors> = compositionLocalOf { Settings.colors }

private val LocalSurfaceHeight: ProvidableCompositionLocal<Int> = compositionLocalOf { 0 }

private val LocalSurfaceBackground: ProvidableCompositionLocal<Color> = compositionLocalOf { error("uninitialized") }

@Composable
fun Modifier.surfaceBackground(shape: Shape = RectangleShape) = background(LocalSurfaceBackground.current, shape)

@Composable
fun Modifier.animatedSurfaceBackground(
    shape: Shape = RectangleShape,
    animationSpec: AnimationSpec<Color> = spring(stiffness = Spring.StiffnessMediumLow),
): Modifier {
    return composed {
        val state = animateColorAsState(targetValue = LocalSurfaceBackground.current, animationSpec = animationSpec)
        background(state.value, shape)
    }
}

@Suppress("MagicNumber")
enum class Colors(
    private val surfaces: Array<Color>,
    val dividerColor: Color,
    val overlay: Color,
    val text: Color,
    val textOnSurface: Color,
    val error: Color,
    val star: Color,
    private val scrollBarHover: Color,
    private val scrollBarUnhover: Color,
    private val materialColors: androidx.compose.material.Colors,
) {
    DARK(
        surfaces = arrayOf(
            Color(0x12, 0x12, 0x12),
            Color(0x24, 0x24, 0x24),
            Color(0x36, 0x36, 0x36),
            Color(0x48, 0x48, 0x48),
            Color(0x50, 0x50, 0x50),
        ),
        dividerColor = Color(0x30, 0x30, 0x30),
        overlay = Color(0x21, 0x21, 0x21).copy(alpha = 0.6f),
        text = Color(0xFA, 0xFA, 0xFA),
        textOnSurface = Color(0x08, 0x08, 0x08),
        error = Color.Red,
        star = Color.Yellow,
        scrollBarHover = Color(0x60, 0x60, 0x60),
        scrollBarUnhover = Color(0x50, 0x50, 0x50),
        materialColors = darkColors(),
    ),

    LIGHT(
        surfaces = arrayOf(
            Color(0xFD, 0xFD, 0xFD),
            Color(0xEF, 0xEF, 0xEF),
            Color(0xE1, 0xE1, 0xE1),
            Color(0xD3, 0xD3, 0xD3),
            Color(0xC5, 0xC5, 0xC5),
        ),
        dividerColor = Color(0x18, 0x18, 0x18),
        overlay = Color(0xEF, 0xEF, 0xEF).copy(alpha = 0.6f),
        text = Color(0x08, 0x08, 0x08),
        textOnSurface = Color(0xFA, 0xFA, 0xFA),
        error = Color.Red,
        star = Color.Yellow,
        scrollBarHover = Color(0x90, 0x90, 0x90),
        scrollBarUnhover = Color(0x78, 0x78, 0x78),
        materialColors = lightColors(),
    );

    val primary: Color
        get() = materialColors.primary

    /**
     * Gets a highlight-aware color, i.e. [primary] if [highlight] is true and [otherwise] if not.
     *
     * Also applies the alpha from [LocalContentAlpha].
     */
    @Composable
    fun highlighted(highlight: Boolean, otherwise: Color = LocalContentColor.current): Color {
        return (if (highlight) primary else otherwise).copy(alpha = LocalContentAlpha.current)
    }

    /**
     * Applies this set of [Colors] to the given [content].
     */
    @Composable
    fun applyColors(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalContentColor provides text,
            LocalScrollbarStyle provides defaultScrollbarStyle().copy(
                hoverColor = scrollBarHover,
                unhoverColor = scrollBarUnhover,
            ),
            LocalSurfaceBackground provides surfaces.first(),
            LocalColors provides this,
        ) {
            MaterialTheme(colors = materialColors, content = content)
        }
    }

    @Composable
    fun withSurface(increment: Int = INCREMENT_SMALL, content: @Composable () -> Unit) {
        val height = LocalSurfaceHeight.current + increment
        val background = surfaces.getOrNull(height)
            ?: error("no surface background for height $height")
        CompositionLocalProvider(
            LocalSurfaceHeight provides height,
            LocalSurfaceBackground provides background,
            content = content,
        )
    }

    companion object {
        const val INCREMENT_SMALL = 1
        const val INCREMENT_LARGE = 2
    }
}
