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

/**
 * Formats this [Duration] as a technical duration of "short" length, i.e. as seconds (if >= 1second) milliseconds.
 *
 * @param decimals number of decimal places to include for milliseconds
 */
@Suppress("MagicNumber")
fun Duration.formatShortDuration(decimals: Int = 0): String {
    return toComponents { seconds, nanoseconds ->
        buildString {
            if (seconds != 0L) {
                append("${seconds}s ")
            }

            if (decimals == 0) {
                append("${nanoseconds / 1_000_000}ms")
            } else {
                append("%.${decimals}fms".format(nanoseconds.toDouble() / 1_000_000))
            }
        }
    }
}
