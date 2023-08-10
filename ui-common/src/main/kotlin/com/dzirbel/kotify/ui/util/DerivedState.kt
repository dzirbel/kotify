package com.dzirbel.kotify.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember

/**
 * Convenience function to produce a derived [State] by applying [transform] to the value of [State].
 */
@Composable
fun <T, R> State<T>.derived(transform: (T) -> R): State<R> {
    return remember(this) { derivedStateOf { transform(this.value) } }
}
