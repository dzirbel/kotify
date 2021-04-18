package com.dzirbel.kotify.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.dzirbel.kotify.util.formatTimeRelativeWithUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

/**
 * Returns an auto-updating relative date text for the given [timestamp].
 *
 * That is, this function launches a background [flow] which updates the relative text as often as necessary, i.e. every
 * second if the relative text is in second granularity, every minute if minute granularity, etc. This flow is saved as
 * a state in the composition and returned, so that it will be recomposed on every update.
 *
 * [format] may also be provided to apply a transformation to the text; this allows it to happen in the flow rather than
 * in the composition.
 */
@Composable
fun liveRelativeDateText(timestamp: Long, format: (String) -> String = { it }): String {
    return remember(timestamp) {
        flow {
            while (true) {
                val (text, unit) = formatTimeRelativeWithUnit(timestamp = timestamp)

                emit(format(text))

                // TODO ideally we might compensate for time taken to format and emit to avoid falling behind, but this
                //  almost always works as intended
                delay(unit.toMillis(1))
            }
        }
    }
        // TODO initial value is not really used since the flow emits immediately, but may trigger a recomposition
        .collectAsState(initial = "")
        .value
}
