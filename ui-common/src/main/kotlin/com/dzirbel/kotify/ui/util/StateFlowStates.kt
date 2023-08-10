package com.dzirbel.kotify.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull

/**
 * Returns a [State] which reflects the first non-null value produced by this [StateFlow].
 *
 * This avoids collecting the [StateFlow] indefinitely (unless it never emits a non-null value), or even collecting it
 * at all if the initial value is non-null.
 */
@Composable
fun <T : Any> StateFlow<T?>.firstAsState(): State<T?> {
    val initialValue = remember(this) { value }
    return if (initialValue == null) {
        produceState<T?>(initialValue = null, key1 = this) {
            value = firstOrNull { it != null }
        }
    } else {
        remember(this) { mutableStateOf(initialValue) }
    }
}
