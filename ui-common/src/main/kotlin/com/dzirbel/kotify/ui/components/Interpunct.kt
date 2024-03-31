package com.dzirbel.kotify.ui.components

import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

@Composable
fun Interpunct(modifier: Modifier = Modifier, style: TextStyle = LocalTextStyle.current) {
    Text(text = "·", modifier = modifier, style = style)
}
