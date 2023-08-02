package com.dzirbel.kotify.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope

/**
 * A simple wrapper around [remember] which includes a [CoroutineScope] local to this point in the composition in the
 * calculation function.
 */
@Composable
inline fun <T> rememberWithCoroutineScope(
    key: Any?,
    crossinline calculation: @DisallowComposableCalls (CoroutineScope) -> T,
): T {
    val scope = rememberCoroutineScope()
    return remember(key) { calculation(scope) }
}
