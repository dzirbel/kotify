package com.dzirbel.kotify.util.time

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class FormatDurationTest {
    data class MediumDurationFormatData(val duration: Duration, val formatted: String)
    data class ShortDurationFormatData(val duration: Duration, val formatted: String, val decimals: Int)

    @ParameterizedTest
    @MethodSource
    fun formatMediumDuration(data: MediumDurationFormatData) {
        assertThat(data.duration.formatMediumDuration()).isEqualTo(data.formatted)
    }

    @ParameterizedTest
    @MethodSource
    fun formatShortDuration(data: ShortDurationFormatData) {
        assertThat(data.duration.formatShortDuration(decimals = data.decimals)).isEqualTo(data.formatted)
    }

    companion object {
        @JvmStatic
        fun formatMediumDuration(): List<MediumDurationFormatData> {
            return listOf(
                MediumDurationFormatData(Duration.ZERO, "0 min"),
                MediumDurationFormatData(1.seconds, "1 sec"),
                MediumDurationFormatData(2.seconds, "2 secs"),
                MediumDurationFormatData(1.minutes, "1 min"),
                MediumDurationFormatData(1.minutes + 1.seconds, "1 min, 1 sec"),
                MediumDurationFormatData(1.minutes + 2.seconds, "1 min, 2 secs"),
                MediumDurationFormatData(1.hours, "1 hour"),
                MediumDurationFormatData(1.hours + 1.seconds, "1 hour, 1 sec"),
                MediumDurationFormatData(1.hours + 1.minutes + 1.seconds, "1 hour, 1 min, 1 sec"),
                MediumDurationFormatData(2.hours + 2.minutes + 2.seconds, "2 hours, 2 mins, 2 secs"),
                MediumDurationFormatData(100.hours, "100 hours"),
            )
        }

        @JvmStatic
        fun formatShortDuration(): List<ShortDurationFormatData> {
            return listOf(
                ShortDurationFormatData(Duration.ZERO, "0ms", decimals = 0),
                ShortDurationFormatData(Duration.ZERO, "0.00ms", decimals = 2),
                ShortDurationFormatData(1.milliseconds + 234.microseconds, "1ms", decimals = 0),
                ShortDurationFormatData(1.milliseconds + 234.microseconds, "1.2ms", decimals = 1),
                ShortDurationFormatData(1.milliseconds + 234.microseconds, "1.23ms", decimals = 2),
                ShortDurationFormatData(1.seconds + 2.milliseconds + 300.microseconds, "1s 2.3ms", decimals = 1),
                ShortDurationFormatData(4.seconds + 2.milliseconds + 300.microseconds, "4s 2ms", decimals = 0),
            )
        }
    }
}
