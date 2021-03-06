package com.dominiczirbel.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// TODO use MaterialTheme directly?
data class Colors(
    val panelBackground: Color,
    val contentBackground: Color,
    val dividerColor: Color,
    val scrollBarHover: Color,
    val scrollBarUnhover: Color,
    val text: Color,
) {
    companion object {
        val dark = Colors(
            panelBackground = Color(0x42, 0x42, 0x42),
            contentBackground = Color(0x21, 0x21, 0x21),
            dividerColor = Color(0x30, 0x30, 0x30),
            scrollBarHover = Color(0x60, 0x60, 0x60),
            scrollBarUnhover = Color(0x50, 0x50, 0x50),
            text = Color(0xFA, 0xFA, 0xFA),
        )

        var current by mutableStateOf(dark)
    }
}

/**
 * Returns a copy of this [Color] which should be used for disabled UI elements.
 */
@Suppress("MagicNumber")
fun Color.disabled(): Color = copy(alpha = 0.5f)
