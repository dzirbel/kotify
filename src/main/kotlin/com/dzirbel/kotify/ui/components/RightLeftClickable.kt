package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.mouseClickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.isSecondaryPressed

/**
 * A [Modifier] which handles right and left clicks; unlike [mouseClickable] it also retains the left click effects of
 * [clickable] (highlight on hover and splash effect on tap).
 */
fun Modifier.rightLeftClickable(
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
): Modifier {
    return this
        .mouseClickable {
            if (buttons.isSecondaryPressed) onRightClick()
        }
        .clickable(onClick = onLeftClick)
}
