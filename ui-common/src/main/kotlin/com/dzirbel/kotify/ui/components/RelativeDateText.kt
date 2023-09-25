package com.dzirbel.kotify.ui.components

import androidx.compose.runtime.Composable
import com.dzirbel.kotify.ui.util.RelativeTimeInfo
import com.dzirbel.kotify.ui.util.iterativeState
import java.util.concurrent.TimeUnit

private const val MILLIS_IN_SECOND = 1_000L

/**
 * Returns an auto-updating [RelativeTimeInfo] for the given [timestamp].
 *
 * That is, this function launches a background job which updates a State reflecting the [RelativeTimeInfo] as often as
 * necessary, i.e. every second if the relative text is in second granularity, every minute if minute granularity, etc.
 */
@Composable
fun liveRelativeTime(timestamp: Long): RelativeTimeInfo {
    return iterativeState(key = timestamp) {
        val relative = RelativeTimeInfo.of(timestamp = timestamp)

        // for second granularity, using the exact amount of time until the next increment tends to fluctuate the
        // updates, so we can just delay by one second each time
        val delay = if (relative.unit == TimeUnit.SECONDS) MILLIS_IN_SECOND else relative.msUntilNextIncrement

        Pair(relative, delay)
    }
}
