package com.dzirbel.kotify.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.svgResource
import com.dzirbel.kotify.ui.theme.Colors
import com.dzirbel.kotify.ui.theme.Dimens

@Composable
fun ThemeSwitcher(modifier: Modifier = Modifier) {
    val isLight = Colors.current == Colors.LIGHT
    IconButton(
        modifier = modifier,
        onClick = {
            Colors.current = if (isLight) Colors.DARK else Colors.LIGHT
        }
    ) {
        Icon(
            painter = svgResource(if (isLight) "wb_sunny.svg" else "nightlight_round.svg"),
            contentDescription = "Theme",
            modifier = Modifier.size(Dimens.iconMedium)
        )
    }
}
