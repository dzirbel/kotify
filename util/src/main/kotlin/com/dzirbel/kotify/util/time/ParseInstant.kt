package com.dzirbel.kotify.util.time

import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * Attempts to [Instant.parse] the given [text], returning null if parsing fails.
 */
fun parseInstantOrNull(text: String): Instant? {
    return try {
        Instant.parse(text)
    } catch (_: DateTimeParseException) {
        null
    }
}
