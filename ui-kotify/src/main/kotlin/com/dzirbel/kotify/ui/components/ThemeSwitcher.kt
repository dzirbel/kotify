package com.dzirbel.kotify.ui.components

import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.theme.KotifyColors

@Composable
fun ThemeSwitcher(modifier: Modifier = Modifier, onSetColors: (KotifyColors) -> Unit) {
    val isLight = KotifyColors.current == KotifyColors.LIGHT
    IconButton(
        modifier = modifier,
        onClick = { onSetColors(if (isLight) KotifyColors.DARK else KotifyColors.LIGHT) },
    ) {
        CachedIcon(
            name = if (isLight) "wb-sunny" else "nightlight-round",
            contentDescription = "Theme",
        )
    }
}
