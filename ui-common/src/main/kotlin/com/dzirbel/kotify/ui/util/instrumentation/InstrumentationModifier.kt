package com.dzirbel.kotify.ui.util.instrumentation

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.Runtime

/**
 * Applies instrumentation to this Composable.
 *
 * This exposes information about the compositions, layouts, and draws of the composable and is intended to help debug
 * performance and correctness, especially of complicated (lazy, subcomposed, etc) Composables.
 *
 * @param tag an optional name associated with the Composable (in logs, etc); by default the name of the function
 *  calling this one
 */
@Stable
fun Modifier.instrument(tag: String? = if (Runtime.debug) callingFunctionName() else null): Modifier {
    return if (Runtime.debug) {
        this
            .highlightCompositions()
            .metricsPanel(tag = tag)
    } else {
        this
    }
}
