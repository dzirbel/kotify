package com.dzirbel.kotify.ui.util.instrumentation

import androidx.compose.runtime.Stable

/**
 * Retrieves the name of the function invoking the one calling this function from the call frame stack.
 *
 * This is useful to log the names of Composables.
 */
@Stable
fun callingFunctionName(): String? {
    return StackWalker.getInstance().walk { frames ->
        // skip two frames: this function and the one calling it
        frames.skip(2).findFirst().map { it.methodName }.orElse(null)
    }
}
