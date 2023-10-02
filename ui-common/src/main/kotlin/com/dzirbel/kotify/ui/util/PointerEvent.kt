package com.dzirbel.kotify.ui.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.collections.immutable.ImmutableSet

/**
 * Convenience alternative to [androidx.compose.ui.input.pointer.onPointerEvent] which accepts multiple [eventTypes].
 */
fun Modifier.onPointerEvent(
    eventTypes: ImmutableSet<PointerEventType>,
    pass: PointerEventPass = PointerEventPass.Main,
    onEvent: AwaitPointerEventScope.(event: PointerEvent) -> Unit,
): Modifier = composed {
    val currentEventTypes by rememberUpdatedState(eventTypes)
    val currentOnEvent by rememberUpdatedState(onEvent)
    pointerInput(pass) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(pass)
                if (event.type in currentEventTypes) {
                    currentOnEvent(event)
                }
            }
        }
    }
}
