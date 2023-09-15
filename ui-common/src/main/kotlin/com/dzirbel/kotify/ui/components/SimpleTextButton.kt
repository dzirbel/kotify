package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.KotifyColors
import com.dzirbel.kotify.ui.util.applyIf

/**
 * A wrapper around [TextButton] which applies standard colors, padding, and corners.
 */
@Composable
fun SimpleTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(Dimens.space3),
    enforceMinWidth: Boolean = true,
    enforceMinHeight: Boolean = false,
    shape: Shape = RectangleShape,
    colors: ButtonColors = if (selected) {
        ButtonDefaults.textButtonColors(
            backgroundColor = KotifyColors.current.selectedBackground,
            contentColor = MaterialTheme.colors.onBackground,
        )
    } else {
        ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colors.onBackground,
        )
    },
    content: @Composable RowScope.() -> Unit,
) {
    TextButton(
        modifier = modifier
            .applyIf(!enforceMinWidth) { widthIn(min = 1.dp) }
            .applyIf(!enforceMinHeight) { heightIn(min = 1.dp) },
        enabled = enabled,
        contentPadding = contentPadding,
        shape = shape,
        colors = colors,
        onClick = onClick,
        content = {
            ProvideTextStyle(MaterialTheme.typography.body2) {
                content()
            }
        },
    )
}
