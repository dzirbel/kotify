package com.dzirbel.kotify.ui.components

import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle

@Composable
fun Interpunct(style: TextStyle = LocalTextStyle.current) {
    Text(text = "·", style = style)
}
