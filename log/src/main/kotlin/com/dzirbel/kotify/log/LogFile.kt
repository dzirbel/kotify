package com.dzirbel.kotify.log

import com.dzirbel.kotify.util.CurrentTime
import com.dzirbel.kotify.util.time.formatShortDuration
import java.io.File
import java.io.IOException
import java.io.Writer
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// TODO capture global errors (UI, etc) and write to the log file
// TODO flush on app exit?
object LogFile {
    private val filenameDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")
    private val logDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    private val maxEventTypeLength = Log.Event.Type.entries.maxOf { it.name.length }
    private var writer: Writer? = null

    fun initialize(directory: File) {
        val now = LocalDateTime.ofInstant(CurrentTime.instant, CurrentTime.zoneId)
        val filename = "log-${filenameDateFormat.format(now)}.txt"
        val file = directory.resolve(filename)

        val success = try {
            file.createNewFile()
        } catch (exception: IOException) {
            System.err.println("Could not create log file $file")
            exception.printStackTrace()
            false
        }

        if (success) {
            writer = file.bufferedWriter()
        }
    }

    internal fun <T> writeEvent(
        log: Log<T>,
        event: Log.Event<T>,
        includeData: Boolean = true,
        includeContent: Boolean = true,
    ) {
        writeLine {
            formatEvent(log = log, event = event, includeData = includeData, includeContent = includeContent)
        }
    }

    internal fun <T> formatEvent(
        log: Log<T>,
        event: Log.Event<T>,
        includeData: Boolean = true,
        includeContent: Boolean = true,
    ): String {
        val eventLocalDataTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.time), CurrentTime.zoneId)
        return buildString {
            append("[${log.name}] ")

            append(logDateFormat.format(eventLocalDataTime))
            append(" ")

            append(event.type.name.padEnd(maxEventTypeLength))
            append(" ")

            append(event.title)

            if (event.duration != null) {
                @Suppress("MagicNumber")
                append(" (${event.duration.formatShortDuration(decimals = 3)})")
            }

            if (includeContent && !event.content.isNullOrBlank()) {
                appendLineIfMultiline(event.content)
            }

            if (includeData && event.data != null && event.data != Unit) {
                appendLineIfMultiline(event.data.toString())
            }
        }
    }

    internal fun writeLine(message: () -> String) {
        if (writer == null) return

        try {
            writer?.write(message() + '\n')
        } catch (exception: IOException) {
            System.err.println("Exception writing to log file")
            System.err.println("  > $message")
            exception.printStackTrace()
        }
    }

    private fun StringBuilder.appendLineIfMultiline(string: String) {
        val lines = string.split('\n')
        if (lines.size == 1) {
            append(" | $string")
        } else {
            appendLine()
            append(lines.joinToString(separator = "\n") { "    $it" })
        }
    }
}
