package com.dominiczirbel.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.TimeZone
import java.util.concurrent.TimeUnit

internal class StringUtilsTest {
    data class ByteSizeCase(val bytes: Long, val formatted: String)
    data class FormatDateTimeCase(
        val timestamp: Long,
        val formatted: String,
        val includeDate: Boolean,
        val includeMillis: Boolean
    )

    data class FormatTimeRelativeCase(val timestamp: Long, val now: Long, val formatted: String)

    @ParameterizedTest
    @MethodSource
    fun formatByteSize(case: ByteSizeCase) {
        assertThat(formatByteSize(case.bytes)).isEqualTo(case.formatted)
    }

    @ParameterizedTest
    @MethodSource
    fun formatDateTime(case: FormatDateTimeCase) {
        assertThat(
            formatDateTime(
                timestamp = case.timestamp,
                includeDate = case.includeDate,
                includeMillis = case.includeMillis
            )
        ).isEqualTo(case.formatted)
    }

    @ParameterizedTest
    @MethodSource
    fun formatTimeRelative(case: FormatTimeRelativeCase) {
        assertThat(formatTimeRelative(timestamp = case.timestamp, now = case.now)).isEqualTo(case.formatted)
    }

    companion object {
        @BeforeAll
        @JvmStatic
        @Suppress("unused")
        fun setup() {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
        }

        @JvmStatic
        @Suppress("unused")
        fun formatByteSize(): List<ByteSizeCase> {
            return listOf(
                ByteSizeCase(bytes = 0, formatted = "0 B"),
                ByteSizeCase(bytes = 1, formatted = "1 B"),
                ByteSizeCase(bytes = 1_023, formatted = "1023 B"),
                ByteSizeCase(bytes = 1_024, formatted = "1.0 KiB"),
                ByteSizeCase(bytes = 1_025, formatted = "1.0 KiB"),
                ByteSizeCase(bytes = 1_400, formatted = "1.4 KiB"),
                ByteSizeCase(bytes = 1_449, formatted = "1.4 KiB"),
                ByteSizeCase(bytes = 1_450, formatted = "1.4 KiB"),
                ByteSizeCase(bytes = 1_451, formatted = "1.4 KiB"),
                ByteSizeCase(bytes = 1_500, formatted = "1.5 KiB"),
                ByteSizeCase(bytes = 1_600, formatted = "1.6 KiB"),
                ByteSizeCase(bytes = 2_000, formatted = "2.0 KiB"),
                ByteSizeCase(bytes = 1_000_000, formatted = "976.6 KiB"),
                ByteSizeCase(bytes = 1_048_576, formatted = "1.0 MiB"),
                ByteSizeCase(bytes = 1e10.toLong(), formatted = "9.3 GiB"),
                ByteSizeCase(bytes = 1e11.toLong(), formatted = "93.1 GiB"),
                ByteSizeCase(bytes = 1e12.toLong(), formatted = "931.3 GiB"),
                ByteSizeCase(bytes = 1e13.toLong(), formatted = "9.1 TiB"),
                ByteSizeCase(bytes = 1e16.toLong(), formatted = "8.9 PiB"),
                ByteSizeCase(bytes = Long.MAX_VALUE, formatted = "8.0 EiB"),
            )
        }

        @JvmStatic
        @Suppress("unused")
        fun formatDateTime(): List<FormatDateTimeCase> {
            return listOf(
                FormatDateTimeCase(
                    timestamp = 1_615_772_944_610,
                    formatted = "2021-03-14 18:49:04.610",
                    includeDate = true,
                    includeMillis = true
                ),
                FormatDateTimeCase(
                    timestamp = 1_615_772_944_610,
                    formatted = "18:49:04.610",
                    includeDate = false,
                    includeMillis = true
                ),
                FormatDateTimeCase(
                    timestamp = 1_615_772_944_610,
                    formatted = "2021-03-14 18:49:04",
                    includeDate = true,
                    includeMillis = false
                ),
                FormatDateTimeCase(
                    timestamp = 1_615_772_944_610,
                    formatted = "18:49:04",
                    includeDate = false,
                    includeMillis = false
                )
            )
        }

        @JvmStatic
        @Suppress("unused")
        fun formatTimeRelative(): List<FormatTimeRelativeCase> {
            val base = 1_600_000_000_000
            return listOf(
                FormatTimeRelativeCase(timestamp = base, now = base, formatted = "now"),
                FormatTimeRelativeCase(timestamp = base, now = base + 10, formatted = "<1 second ago"),
                FormatTimeRelativeCase(timestamp = base, now = base - 10, formatted = "in <1 second"),
                FormatTimeRelativeCase(timestamp = base, now = base + 1_000, formatted = "1 second ago"),
                FormatTimeRelativeCase(timestamp = base, now = base - 1_000, formatted = "in 1 second"),
                FormatTimeRelativeCase(timestamp = base, now = base + 1_700, formatted = "1 second ago"),
                FormatTimeRelativeCase(timestamp = base, now = base - 1_700, formatted = "in 1 second"),
                FormatTimeRelativeCase(timestamp = base, now = base + 2_000, formatted = "2 seconds ago"),
                FormatTimeRelativeCase(timestamp = base, now = base - 2_000, formatted = "in 2 seconds"),

                FormatTimeRelativeCase(
                    timestamp = base,
                    now = base + TimeUnit.MINUTES.toMillis(3),
                    formatted = "3 minutes ago"
                ),
                FormatTimeRelativeCase(
                    timestamp = base,
                    now = base - TimeUnit.MINUTES.toMillis(3),
                    formatted = "in 3 minutes"
                ),

                FormatTimeRelativeCase(
                    timestamp = base,
                    now = base + TimeUnit.HOURS.toMillis(4),
                    formatted = "4 hours ago"
                ),
                FormatTimeRelativeCase(
                    timestamp = base,
                    now = base - TimeUnit.HOURS.toMillis(4),
                    formatted = "in 4 hours"
                ),

                FormatTimeRelativeCase(
                    timestamp = base,
                    now = base + TimeUnit.DAYS.toMillis(5),
                    formatted = "5 days ago"
                ),
                FormatTimeRelativeCase(
                    timestamp = base,
                    now = base - TimeUnit.DAYS.toMillis(5),
                    formatted = "in 5 days"
                ),

                FormatTimeRelativeCase(
                    timestamp = base,
                    now = base + TimeUnit.DAYS.toMillis(100),
                    formatted = "100 days ago"
                ),
                FormatTimeRelativeCase(
                    timestamp = base,
                    now = base - TimeUnit.DAYS.toMillis(100),
                    formatted = "in 100 days"
                ),
            )
        }
    }
}
