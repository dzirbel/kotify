package com.dzirbel.kotify.ui.components

import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.Settings
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.theme.Colors
import com.dzirbel.kotify.ui.theme.LocalColors

@Composable
fun ThemeSwitcher(modifier: Modifier = Modifier) {
    val isLight = LocalColors.current == Colors.LIGHT
    IconButton(
        modifier = modifier,
        onClick = {
            Settings.colors = if (isLight) Colors.DARK else Colors.LIGHT
        },
    ) {
        CachedIcon(
            name = if (isLight) "wb-sunny" else "nightlight-round",
            contentDescription = "Theme",
        )
    }
}
