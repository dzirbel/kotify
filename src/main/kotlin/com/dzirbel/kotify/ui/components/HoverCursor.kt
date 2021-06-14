package com.dzirbel.kotify.ui.components

import androidx.compose.desktop.LocalAppWindow
import androidx.compose.ui.Modifier
import java.awt.Cursor

/**
 * A [Modifier] which applies the given [hoverCursor] when the element is hovered.
 */
fun Modifier.hoverCursor(hoverCursor: Cursor): Modifier {
    return hoverState { isHover ->
        LocalAppWindow.current.window.cursor = if (isHover) hoverCursor else Cursor.getDefaultCursor()
    }
}
