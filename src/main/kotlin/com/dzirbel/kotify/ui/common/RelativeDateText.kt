package com.dzirbel.kotify.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import com.dzirbel.kotify.util.RelativeTimeInfo
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

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
    // TODO initial value is not really used and may trigger a recomposition
    return produceState(initialValue = "", key1 = timestamp) {
        while (true) {
            val relative = RelativeTimeInfo.of(timestamp = timestamp)

            value = format(relative.format())

            // for second granularity, using the exact amount of time until the next increment tends to fluctuate the
            // updates, so we can just delay by one second each time
            delay(
                if (relative.unit == TimeUnit.SECONDS) TimeUnit.SECONDS.toMillis(1) else relative.msUntilNextIncrement
            )
        }
    }.value
}
