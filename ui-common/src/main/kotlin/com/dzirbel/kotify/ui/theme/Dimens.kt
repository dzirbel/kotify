package com.dzirbel.kotify.ui.theme

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.material.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

@Stable
object Dimens {
    // space constants - all spacing between elements must use one of these values
    val space1 = 4.dp
    val space2 = 6.dp
    val space3 = 10.dp
    val space4 = 18.dp
    val space5 = 32.dp

    // icon sizes - all icons must either use one of these sizes or a size matching a font size
    val iconTiny = 16.dp
    val iconSmall = 20.dp
    val iconMedium = 32.dp
    val iconLarge = 48.dp

    // rounded corner size - all rounded corners must either use this size or be a "pill" with maximum rounding
    val cornerSize = 4.dp

    // divider size - all dividers between elements must use this width/height
    val divider = 1.dp

    /**
     * Returns the [LocalTextStyle]'s font size in [Dp].
     */
    val fontDp: Dp
        @Composable
        get() = iconSizeFor(LocalTextStyle.current.fontSize)

    // size of common images - album art, artist image, etc
    val contentImage = 200.dp
    val contentImageSmall = 80.dp

    val tooltipMaxWidth: Dp = 500.dp

    val tooltipElevation = 8.dp
    val panelElevationSmall = 4.dp
    val panelElevationLarge = 8.dp
    val componentElevation = 6.dp

    private val scrollbarWidth = 12.dp

    @Composable
    fun ApplyDimens(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.body2,
            LocalScrollbarStyle provides LocalScrollbarStyle.current.copy(thickness = scrollbarWidth),
            LocalMinimumInteractiveComponentEnforcement provides false,
            content = content,
        )
    }

    /**
     * Returns a [Dp] equivalent for the given [fontSize], to be used for icons embedded in text of [fontSize].
     */
    @Composable
    fun iconSizeFor(fontSize: TextUnit): Dp {
        return with(LocalDensity.current) { fontSize.toDp() }
    }
}
