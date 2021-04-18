package com.dzirbel.kotify.ui.util

import androidx.compose.runtime.MutableState

/**
 * A convenience function which sets the [MutableState.value] of this [MutableState] to the value returned by [mutation]
 * on it.
 *
 * This reduces the common case of
 *
 *   myState.value = myState.value.copy(...)
 *
 * to
 *
 *   myState.mutate { copy(...) }
 */
fun <T, R : MutableState<T>> R.mutate(mutation: T.() -> T) {
    value = value.mutation()
}
