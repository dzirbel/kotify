package com.dzirbel.kotify.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.theme.Dimens

@Composable
fun HelpTooltip(tooltip: String, modifier: Modifier = Modifier, size: Dp = Dimens.iconSmall) {
    TooltipArea(tooltip, modifier = modifier) {
        CachedIcon(name = "help", size = size)
    }
}
