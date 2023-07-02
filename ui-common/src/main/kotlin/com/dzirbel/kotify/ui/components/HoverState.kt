package com.dzirbel.kotify.ui.components

import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent

/**
 * Modifies [state] to reflect the hover status of this element, i.e. sets it to true when the pointer enters the
 * element and false when it leaves the element.
 *
 * TODO replace with hoverable() and MutableInteractionSource
 */
fun Modifier.hoverState(state: MutableState<Boolean>): Modifier {
    return this
        .onPointerEvent(PointerEventType.Enter) { state.value = true }
        .onPointerEvent(PointerEventType.Exit) { state.value = false }
}
