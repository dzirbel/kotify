package com.dzirbel.kotify.ui.theme

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Dimens {
    // space constants - all spacing between elements must use one of these values
    val space1 = 2.dp
    val space2 = 4.dp
    val space3 = 8.dp
    val space4 = 16.dp
    val space5 = 32.dp

    // icon sizes - all icons must either use one of these sizes or a size matching a font size
    val iconTiny = 14.dp
    val iconSmall = 20.dp
    val iconMedium = 32.dp
    val iconLarge = 48.dp
    val iconHuge = 96.dp

    // rounded corner size - all rounded corners must either use this size or be a "pill" with maximum rounding
    val cornerSize = 4.dp

    // divider size - all dividers between elements must use this width/height
    val divider = 1.dp

    // font sizes - all text must use one of these sizes
    val fontTitle = 24.sp
    val fontBody = 14.sp
    val fontSmall = 12.sp
    val fontCaption = 10.sp

    private var fontDpInitialized = false
    var fontBodyDp: Dp = 0.dp
        private set

    // size of common images - album art, artist image, etc
    val contentImage = 200.dp
    val contentImageSmall = 80.dp

    private val scrollbarWidth = 12.dp

    @Composable
    fun applyDimens(content: @Composable () -> Unit) {
        // convenience calculation of dp sizes of fonts, for easy of use outside of composable context
        if (!fontDpInitialized) {
            fontDpInitialized = true
            fontBodyDp = with(LocalDensity.current) { fontBody.toDp() }
        }

        CompositionLocalProvider(
            LocalTextStyle provides TextStyle(fontSize = fontBody),
            LocalScrollbarStyle provides LocalScrollbarStyle.current.copy(thickness = scrollbarWidth),
            content = content
        )
    }
}
