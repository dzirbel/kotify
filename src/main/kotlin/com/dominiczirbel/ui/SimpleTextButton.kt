package com.dominiczirbel.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dominiczirbel.ui.theme.Colors
import com.dominiczirbel.ui.theme.Dimens

/**
 * A wrapper around [TextButton] which applies standard colors, padding, and corners.
 */
@Composable
fun SimpleTextButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = Color.Transparent,
    textColor: Color = if (backgroundColor == Color.Transparent) Colors.current.text else Colors.current.textOnSurface,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    TextButton(
        modifier = modifier,
        enabled = enabled,
        contentPadding = PaddingValues(Dimens.space3),
        shape = RoundedCornerShape(0.dp),
        colors = ButtonDefaults.textButtonColors(
            backgroundColor = backgroundColor,
            contentColor = textColor,
            disabledContentColor = textColor.copy(alpha = LocalContentAlpha.current)
        ),
        onClick = onClick,
        content = content
    )
}
