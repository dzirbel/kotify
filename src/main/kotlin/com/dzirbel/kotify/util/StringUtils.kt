package com.dzirbel.kotify.util

import java.lang.Long.signum
import java.text.StringCharacterIterator
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

private val formatDateTimeMillis = DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss.SSS")
private val formatDateTime = DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss")
private val formatDate = DateTimeFormatter.ofPattern("YYYY-MM-dd")
private val formatTime = DateTimeFormatter.ofPattern("HH:mm:ss")
private val formatTimeMillis = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

/**
 * Returns a copy of this string trimmed to [maxChars] with an ellipsis (...) appended if the original string exceeded
 * [maxChars].
 */
fun String.ellipsize(maxChars: Int): String {
    @Suppress("MagicNumber")
    return if (length > maxChars) take(maxChars - 3) + "..." else this
}

/**
 * Returns a copy of this string with its first character capitalized.
 */
fun String.capitalize(locale: Locale = Locale.getDefault()): String {
    return replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
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
    includeMillis: Boolean = false,
    locale: Locale = Locale.getDefault(),
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val formatter = when {
        includeDate && includeTime && includeMillis -> formatDateTimeMillis
        includeDate && includeTime && !includeMillis -> formatDateTime
        includeDate && !includeTime && includeMillis -> error("unsupported: cannot include millis without time")
        !includeDate && includeTime && includeMillis -> formatTimeMillis
        includeDate && !includeTime && !includeMillis -> formatDate
        !includeDate && includeTime && !includeMillis -> formatTime
        else -> error("unsupported: must include some field")
    }

    return formatter.withLocale(locale).withZone(zone).format(Instant.ofEpochMilli(timestamp))
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
