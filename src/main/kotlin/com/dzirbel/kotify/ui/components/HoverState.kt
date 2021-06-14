package com.dzirbel.kotify.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerMoveFilter

fun Modifier.hoverState(state: MutableState<Boolean>): Modifier {
    return composed {
        pointerMoveFilter(
            onEnter = { true.also { state.value = true } },
            onExit = { true.also { state.value = false } }
        )
    }
}

fun Modifier.hoverState(onHover: @Composable (Boolean) -> Unit): Modifier {
    return composed {
        val state = remember { mutableStateOf(false) }

        onHover(state.value)

        pointerMoveFilter(
            onEnter = { true.also { state.value = true } },
            onExit = { true.also { state.value = false } }
        )
    }
}
