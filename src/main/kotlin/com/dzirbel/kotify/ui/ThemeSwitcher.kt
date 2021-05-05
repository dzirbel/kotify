package com.dzirbel.kotify.ui

import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.ui.theme.Colors

@Composable
fun ThemeSwitcher(modifier: Modifier = Modifier) {
    val isLight = Colors.current == Colors.LIGHT
    IconButton(
        modifier = modifier,
        onClick = {
            Colors.current = if (isLight) Colors.DARK else Colors.LIGHT
        }
    ) {
        CachedIcon(
            name = if (isLight) "wb-sunny" else "nightlight-round",
            contentDescription = "Theme"
        )
    }
}
