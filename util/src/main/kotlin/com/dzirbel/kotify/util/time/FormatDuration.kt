package com.dzirbel.kotify.util.time

import com.dzirbel.kotify.util.formattedWithUnit
import com.dzirbel.kotify.util.takingIf
import kotlin.time.Duration

/**
 * Formats this [Duration] as a user-readable duration of "medium" length, i.e. as whole seconds, minutes, and hours.
 */
fun Duration.formatMediumDuration(): String {
    if (inWholeSeconds == 0L) {
        return "0 min"
    }

    return toComponents { hours, minutes, seconds, _ ->
        listOfNotNull(
            takingIf(hours != 0L) { hours.formattedWithUnit("hour") },
            takingIf(minutes != 0) { minutes.formattedWithUnit("min") },
            takingIf(seconds != 0) { seconds.formattedWithUnit("sec") },
        ).joinToString()
    }
}
