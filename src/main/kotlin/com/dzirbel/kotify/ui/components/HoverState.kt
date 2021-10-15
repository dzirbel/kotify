package com.dzirbel.kotify.ui.components

import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerMoveFilter

/**
 * Modifies [state] to reflect the hover status of this element, i.e. sets it to true when the pointer enters the
 * element and false when it leaves the element.
 */
fun Modifier.hoverState(state: MutableState<Boolean>): Modifier {
    return pointerMoveFilter(
        onEnter = { true.also { state.value = true } },
        onExit = { true.also { state.value = false } }
    )
}
