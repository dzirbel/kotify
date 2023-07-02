package com.dzirbel.kotify.ui.util.instrumentation

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

/**
 * Applies instrumentation to this Composable.
 *
 * This exposes information about the compositions, layouts, and draws of the composable and is intended to help debug
 * performance and correctness, especially of complicated (lazy, subcomposed, etc) Composables.
 *
 * TODO disable implementation with no overhead for release builds
 *
 * @param tag an optional name associated with the Composable (in logs, etc); by default the name of the function
 *  calling this one
 */
@Stable
fun Modifier.instrument(
    tag: String? = callingFunctionName(), // TODO avoid retrieving the function name when the metrics panel is disabled
): Modifier {
    return this
        .highlightCompositions()
        .metricsPanel(tag = tag)
}
