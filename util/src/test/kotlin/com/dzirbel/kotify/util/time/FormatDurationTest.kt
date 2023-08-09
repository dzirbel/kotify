package com.dzirbel.kotify.util.time

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class FormatDurationTest {
    data class DurationFormatData(val duration: Duration, val formatted: String)

    @ParameterizedTest
    @MethodSource
    fun formatMediumDuration(data: DurationFormatData) {
        assertThat(data.duration.formatMediumDuration()).isEqualTo(data.formatted)
    }

    companion object {
        @JvmStatic
        fun formatMediumDuration(): List<DurationFormatData> {
            return listOf(
                DurationFormatData(Duration.ZERO, "0 min"),
                DurationFormatData(1.seconds, "1 sec"),
                DurationFormatData(2.seconds, "2 secs"),
                DurationFormatData(1.minutes, "1 min"),
                DurationFormatData(1.minutes + 1.seconds, "1 min, 1 sec"),
                DurationFormatData(1.minutes + 2.seconds, "1 min, 2 secs"),
                DurationFormatData(1.hours, "1 hour"),
                DurationFormatData(1.hours + 1.seconds, "1 hour, 1 sec"),
                DurationFormatData(1.hours + 1.minutes + 1.seconds, "1 hour, 1 min, 1 sec"),
                DurationFormatData(2.hours + 2.minutes + 2.seconds, "2 hours, 2 mins, 2 secs"),
                DurationFormatData(100.hours, "100 hours"),
            )
        }
    }
}
