package com.dzirbel.kotify.ui.components

import androidx.compose.runtime.Composable
import com.dzirbel.kotify.ui.util.RelativeTimeInfo
import com.dzirbel.kotify.ui.util.iterativeState
import java.util.concurrent.TimeUnit

private const val MILLIS_IN_SECOND = 1_000L

/**
 * Returns an auto-updating relative date text for the given [timestamp].
 *
 * That is, this function launches a background job which updates the relative text as often as necessary, i.e. every
 * second if the relative text is in second granularity, every minute if minute granularity, etc. This job is saved as
 * a state in the composition and returned, so that it will be recomposed on every update.
 *
 * [format] may also be provided to apply a transformation to the text; this allows it to happen in the job rather than
 * in the composition.
 */
@Composable
fun liveRelativeDateText(timestamp: Long, format: (String) -> String = { it }): String {
    return iterativeState(key = timestamp) {
        val relative = RelativeTimeInfo.of(timestamp = timestamp)
        val value = format(relative.format())

        // for second granularity, using the exact amount of time until the next increment tends to fluctuate the
        // updates, so we can just delay by one second each time
        val delay = if (relative.unit == TimeUnit.SECONDS) MILLIS_IN_SECOND else relative.msUntilNextIncrement

        Pair(value, delay)
    }
}
