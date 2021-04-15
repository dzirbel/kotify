package com.dominiczirbel.util

import java.lang.Long.signum
import java.text.SimpleDateFormat
import java.text.StringCharacterIterator
import java.util.concurrent.TimeUnit
import kotlin.math.abs

private val formatDateTimeMillis = SimpleDateFormat("YYYY-MM-dd HH:mm:ss.SSS")
private val formatDateTime = SimpleDateFormat("YYYY-MM-dd HH:mm:ss")
private val formatDate = SimpleDateFormat("YYYY-MM-dd")
private val formatTime = SimpleDateFormat("HH:mm:ss")
private val formatTimeMillis = SimpleDateFormat("HH:mm:ss.SSS")

/**
 * Returns a copy of this string trimmed to [maxChars] with an ellipsis (...) appended if the original string exceeded
 * [maxChars].
 */
fun String.ellipsize(maxChars: Int): String {
    @Suppress("MagicNumber")
    return if (length > maxChars) take(maxChars - 3) + "..." else this
}

/**
 * Returns a human-readable format of the given file size in bytes.
 *
 * From https://stackoverflow.com/a/3758880
 */
@Suppress("ImplicitDefaultLocale", "MagicNumber", "UnderscoresInNumericLiterals")
fun formatByteSize(bytes: Long): String {
    if (bytes < 1024) {
        return "$bytes B"
    }
    var value = bytes
    val ci = StringCharacterIterator("KMGTPE")
    var i = 40
    while (i >= 0 && bytes > 0xfffccccccccccccL shr i) {
        value = value shr 10
        ci.next()
        i -= 10
    }
    value *= signum(bytes).toLong()
    return String.format("%.1f %ciB", value / 1024.0, ci.current())
}

/**
 * Returns an absolute format of the given [timestamp], e.g. "2021-01-01 12:34:56".
 */
fun formatDateTime(
    timestamp: Long,
    includeDate: Boolean = true,
    includeTime: Boolean = true,
    includeMillis: Boolean = false
): String {
    val format = when {
        includeDate && includeTime && includeMillis -> formatDateTimeMillis
        includeDate && includeTime && !includeMillis -> formatDateTime
        includeDate && !includeTime && includeMillis -> error("unsupported: cannot include millis without time")
        !includeDate && includeTime && includeMillis -> formatTimeMillis
        includeDate && !includeTime && !includeMillis -> formatDate
        !includeDate && includeTime && !includeMillis -> formatTime
        !includeDate && !includeTime && includeMillis -> error("unsupported: cannot include only millis")
        else -> error("unsupported: must include some field")
    }

    return format.format(timestamp)
}

/**
 * Returns a relative format of the given [timestamp] relative to [now], e.g. "3 minutes ago", and the [TimeUnit] of the
 * granularity.
 */
fun formatTimeRelativeWithUnit(timestamp: Long, now: Long = System.currentTimeMillis()): Pair<String, TimeUnit> {
    if (timestamp == now) {
        return Pair("now", TimeUnit.SECONDS)
    }

    val difference = abs(timestamp - now)

    var amount = TimeUnit.MILLISECONDS.toDays(difference)
    val unit = if (amount > 0) {
        TimeUnit.DAYS
    } else {
        amount = TimeUnit.MILLISECONDS.toHours(difference)
        if (amount > 0) {
            TimeUnit.HOURS
        } else {
            amount = TimeUnit.MILLISECONDS.toMinutes(difference)
            if (amount > 0) {
                TimeUnit.MINUTES
            } else {
                amount = TimeUnit.MILLISECONDS.toSeconds(difference)
                TimeUnit.SECONDS
            }
        }
    }

    val timeUnitName = when (unit) {
        TimeUnit.SECONDS -> "second"
        TimeUnit.MINUTES -> "minute"
        TimeUnit.HOURS -> "hour"
        TimeUnit.DAYS -> "day"
        else -> error("unexpected TimeUnit $unit")
    }

    val unitFormatted = if (amount <= 1L) timeUnitName else timeUnitName + "s"
    val amountFormatted = if (amount == 0L) "<1" else amount.toString()

    val text = if (timestamp < now) "$amountFormatted $unitFormatted ago" else "in $amountFormatted $unitFormatted"
    return Pair(text, unit)
}

/**
 * Returns a relative format of the given [timestamp] relative to [now], e.g. "3 minutes ago".
 */
fun formatTimeRelative(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    return formatTimeRelativeWithUnit(timestamp = timestamp, now = now).first
}

/**
 * Returns a duration format of the given [durationMs], e.g. "3:14" for 3 minutes and 14 seconds.
 */
@Suppress("MagicNumber")
fun formatDuration(durationMs: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60

    val hoursString = if (hours > 0) "$hours:" else ""
    val minutesString = if (hours > 0) minutes.toString().padStart(length = 2, padChar = '0') else minutes.toString()
    val secondsString = seconds.toString().padStart(length = 2, padChar = '0')

    return "$hoursString$minutesString:$secondsString"
}
