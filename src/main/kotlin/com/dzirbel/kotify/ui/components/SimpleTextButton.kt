package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.ui.theme.Colors
import com.dzirbel.kotify.ui.theme.Dimens

/**
 * A wrapper around [TextButton] which applies standard colors, padding, and corners.
 */
@Composable
fun SimpleTextButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(Dimens.space3),
    enforceMinWidth: Boolean = true,
    enforceMinHeight: Boolean = false,
    backgroundColor: Color = Color.Transparent,
    textColor: Color = if (backgroundColor == Color.Transparent) Colors.current.text else Colors.current.textOnSurface,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    TextButton(
        modifier = modifier
            .let {
                if (enforceMinWidth) {
                    it
                } else {
                    it.widthIn(min = 1.dp)
                }
            }
            .let {
                if (enforceMinHeight) {
                    it
                } else {
                    it.heightIn(min = 1.dp)
                }
            },
        enabled = enabled,
        contentPadding = contentPadding,
        shape = RectangleShape,
        colors = ButtonDefaults.textButtonColors(
            backgroundColor = backgroundColor,
            contentColor = textColor,
            disabledContentColor = textColor.copy(alpha = LocalContentAlpha.current)
        ),
        onClick = onClick,
        content = content
    )
}
