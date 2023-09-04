package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.theme.Dimens

@Composable
fun HelpTooltip(tooltip: String, modifier: Modifier = Modifier, size: Dp = Dimens.iconSmall) {
    TooltipArea(
        tooltip = {
            Surface(shape = RoundedCornerShape(Dimens.cornerSize), elevation = Dimens.contextMenuElevation) {
                Text(tooltip, Modifier.padding(Dimens.space2))
            }
        },
    ) {
        CachedIcon(name = "help", modifier = modifier, size = size)
    }
}
