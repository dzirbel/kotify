package com.dominiczirbel.ui.theme

import androidx.compose.foundation.ScrollbarStyleAmbient
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Dimens {
    val space1 = 2.dp
    val space2 = 4.dp
    val space3 = 8.dp
    val space4 = 16.dp
    val space5 = 32.dp

    val iconSmall = 20.dp
    val iconMedium = 32.dp
    val iconLarge = 48.dp

    private val scrollbarWidth = 12.dp

    val divider = 1.dp

    val fontTitle = 24.sp
    val fontBody = 14.sp
    val fontSmall = 12.sp
    val fontCaption = 10.sp

    @Composable
    fun applyDimens(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalTextStyle provides TextStyle(fontSize = fontBody),
            ScrollbarStyleAmbient provides ScrollbarStyleAmbient.current.copy(thickness = scrollbarWidth),
            content = content
        )
    }
}
