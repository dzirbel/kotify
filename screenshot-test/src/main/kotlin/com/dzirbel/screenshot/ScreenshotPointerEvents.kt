package com.dzirbel.screenshot

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.scene.ComposeScenePointer

/**
 * Moves the pointer to hover over the given [x], [y] coordinates.
 */
fun ImageComposeScene.hover(x: Float, y: Float) {
    sendPointerEvent(
        eventType = PointerEventType.Move,
        pointers = listOf(ComposeScenePointer(id = PointerId(0), position = Offset(x = x, y = y), pressed = false)),
    )
}

/**
 * Moves the pointer to hover over the given [x], [y] coordinates and clicks with the primary button.
 */
fun ImageComposeScene.click(x: Float, y: Float) {
    hover(x, y)

    sendPointerEvent(
        eventType = PointerEventType.Press,
        buttons = PointerButtons(isPrimaryPressed = true),
        button = PointerButton.Primary,
        pointers = listOf(ComposeScenePointer(id = PointerId(0), position = Offset(x = x, y = y), pressed = true)),
    )

    sendPointerEvent(
        eventType = PointerEventType.Release,
        button = PointerButton.Primary,
        pointers = listOf(ComposeScenePointer(id = PointerId(0), position = Offset(x = x, y = y), pressed = false)),
    )
}
