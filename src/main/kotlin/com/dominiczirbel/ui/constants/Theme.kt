package com.dominiczirbel.ui.constants

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// TODO use MaterialTheme directly
data class Theme(
    val panelBackground: Color,
    val contentBackground: Color,
    val dividerColor: Color,
    val text: Color,
) {
    companion object {
        val dark = Theme(
            panelBackground = Color(0x42, 0x42, 0x42),
            contentBackground = Color(0x21, 0x21, 0x21),
            dividerColor = Color(0x30, 0x30, 0x30),
            text = Color(0xFA, 0xFA, 0xFA),
        )

        val light = Theme(
            panelBackground = Color(0xEE, 0xEE, 0xEE),
            contentBackground = Color(0xF5, 0xF5, 0xF5),
            dividerColor = Color(0x06, 0x06, 0x06),
            text = Color(0x12, 0x12, 0x12),
        )

        var current by mutableStateOf(dark)
    }
}
