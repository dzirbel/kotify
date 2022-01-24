package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors

/**
 * A simple pill component with the given [text].
 */
@Composable
fun Pill(
    text: String,
    borderColor: Color = LocalColors.current.dividerColor,
    backgroundColor: Color = LocalColors.current.surface1,
    cornerSize: Dp = Dimens.cornerSize,
    padding: PaddingValues = PaddingValues(horizontal = Dimens.space2, vertical = Dimens.space1),
) {
    Text(
        text = text,
        maxLines = 1,
        modifier = Modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(size = cornerSize))
            .border(width = Dimens.divider, color = borderColor, shape = RoundedCornerShape(size = cornerSize))
            .padding(padding),
    )
}
