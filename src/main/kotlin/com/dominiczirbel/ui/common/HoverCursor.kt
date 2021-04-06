package com.dominiczirbel.ui.common

import androidx.compose.desktop.LocalAppWindow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerMoveFilter
import java.awt.Cursor

/**
 * A [Modifier] which applies the given [hoverCursor] when the element is hovered.
 */
fun Modifier.hoverCursor(hoverCursor: Cursor): Modifier {
    return composed {
        var isHover by remember { mutableStateOf(false) }

        LocalAppWindow.current.window.cursor = if (isHover) hoverCursor else Cursor.getDefaultCursor()

        pointerMoveFilter(
            onEnter = { true.also { isHover = true } },
            onExit = { true.also { isHover = false } }
        )
    }
}
