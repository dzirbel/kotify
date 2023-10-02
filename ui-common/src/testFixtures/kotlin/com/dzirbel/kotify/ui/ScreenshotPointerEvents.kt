package com.dzirbel.kotify.ui

import androidx.compose.ui.ComposeScene
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId

/**
 * Moves the pointer to hover over the given [x], [y] coordinates.
 */
fun ImageComposeScene.hover(x: Float, y: Float) {
    sendPointerEvent(
        eventType = PointerEventType.Move,
        pointers = listOf(
            ComposeScene.Pointer(id = PointerId(0L), position = Offset(x = x, y = y), pressed = false),
        ),
    )
}

/**
 * Moves the pointer to hover over the given [x], [y] coordinates and clicks with the primary button.
 */
fun ImageComposeScene.click(x: Float, y: Float) {
    sendPointerEvent(
        eventType = PointerEventType.Move,
        pointers = listOf(
            ComposeScene.Pointer(id = PointerId(0L), position = Offset(x = x, y = y), pressed = false),
        ),
    )

    sendPointerEvent(
        eventType = PointerEventType.Press,
        buttons = PointerButtons(isPrimaryPressed = true),
        button = PointerButton.Primary,
        pointers = listOf(
            ComposeScene.Pointer(id = PointerId(0L), position = Offset(x = x, y = y), pressed = true),
        ),
    )

    sendPointerEvent(
        eventType = PointerEventType.Release,
        button = PointerButton.Primary,
        pointers = listOf(
            ComposeScene.Pointer(id = PointerId(0L), position = Offset(x = x, y = y), pressed = false),
        ),
    )
}
