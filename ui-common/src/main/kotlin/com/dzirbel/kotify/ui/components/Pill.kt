package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.KotifyColors

/**
 * A simple pill component with the given [text].
 */
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    borderColor: Color = KotifyColors.current.divider,
    cornerSize: Dp = Dimens.cornerSize,
    padding: PaddingValues = PaddingValues(horizontal = Dimens.space2, vertical = Dimens.space1),
) {
    Surface(
        modifier = modifier,
        elevation = Dimens.componentElevation,
        shape = RoundedCornerShape(size = cornerSize),
        border = BorderStroke(width = Dimens.divider, color = borderColor),
    ) {
        Text(text = text, maxLines = 1, modifier = Modifier.padding(padding))
    }
}
