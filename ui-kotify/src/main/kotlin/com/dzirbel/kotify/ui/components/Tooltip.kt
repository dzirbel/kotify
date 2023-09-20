package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.ui.theme.Dimens

/**
 * A simplified variant of [TooltipArea] which shows a text [tooltip] on hover of the given [content].
 */
@Composable
fun TooltipArea(tooltip: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    TooltipArea(
        tooltip = {
            Surface(shape = RoundedCornerShape(Dimens.cornerSize), elevation = Dimens.tooltipElevation) {
                Text(
                    text = tooltip,
                    modifier = Modifier.padding(Dimens.space2).widthIn(max = Dimens.tooltipMaxWidth),
                )
            }
        },
        modifier = modifier,
        content = content,
    )
}
