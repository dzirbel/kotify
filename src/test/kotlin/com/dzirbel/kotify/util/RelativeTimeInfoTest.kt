package com.dzirbel.kotify.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.concurrent.TimeUnit

internal class RelativeTimeInfoTest {
    data class RelativeTestCase(val timestamp: Long, val now: Long, val expected: RelativeTimeInfo)
    data class FormatTestCase(val info: RelativeTimeInfo, val format: String)

    @ParameterizedTest
    @MethodSource
    fun testRelative(case: RelativeTestCase) {
        val info = RelativeTimeInfo.of(timestamp = case.timestamp, now = case.now)
        assertThat(info).isEqualTo(case.expected)
    }

    @ParameterizedTest
    @MethodSource
    fun testFormat(case: FormatTestCase) {
        assertThat(case.info.format()).isEqualTo(case.format)
    }

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun testRelative(): List<RelativeTestCase> {
            val base = 1_600_000_000_000
            return listOf(
                RelativeTestCase(
                    timestamp = base,
                    now = base,
                    expected = RelativeTimeInfo(unit = TimeUnit.SECONDS, amount = 0, msUntilNextIncrement = 1_000)
                ),
                RelativeTestCase(
                    timestamp = base + 1,
                    now = base,
                    expected = RelativeTimeInfo(unit = TimeUnit.SECONDS, amount = 0, msUntilNextIncrement = 999)
                ),
                RelativeTestCase(
                    timestamp = base - 1,
                    now = base,
                    expected = RelativeTimeInfo(unit = TimeUnit.SECONDS, amount = 0, msUntilNextIncrement = 1001)
                ),
                RelativeTestCase(
                    timestamp = base + 1001,
                    now = base,
                    expected = RelativeTimeInfo(unit = TimeUnit.SECONDS, amount = 1, msUntilNextIncrement = 999)
                ),
                RelativeTestCase(
                    timestamp = base - 1001,
                    now = base,
                    expected = RelativeTimeInfo(unit = TimeUnit.SECONDS, amount = -1, msUntilNextIncrement = 1)
                ),
                RelativeTestCase(
                    timestamp = base + 3_600,
                    now = base + 2_000,
                    expected = RelativeTimeInfo(unit = TimeUnit.SECONDS, amount = 1, msUntilNextIncrement = 400)
                ),
                RelativeTestCase(
                    timestamp = base + TimeUnit.MINUTES.toMillis(3) + TimeUnit.SECONDS.toMillis(20),
                    now = base,
                    expected = RelativeTimeInfo(
                        unit = TimeUnit.MINUTES,
                        amount = 3,
                        msUntilNextIncrement = TimeUnit.SECONDS.toMillis(40)
                    )
                ),
                RelativeTestCase(
                    timestamp = base,
                    now = base + TimeUnit.MINUTES.toMillis(3) + TimeUnit.SECONDS.toMillis(20),
                    expected = RelativeTimeInfo(
                        unit = TimeUnit.MINUTES,
                        amount = -3,
                        msUntilNextIncrement = TimeUnit.SECONDS.toMillis(20)
                    )
                ),
                RelativeTestCase(
                    timestamp = base + TimeUnit.DAYS.toMillis(100),
                    now = base,
                    expected = RelativeTimeInfo(
                        unit = TimeUnit.DAYS,
                        amount = 100,
                        msUntilNextIncrement = TimeUnit.DAYS.toMillis(1)
                    )
                ),
                RelativeTestCase(
                    timestamp = base - TimeUnit.DAYS.toMillis(100),
                    now = base,
                    expected = RelativeTimeInfo(
                        unit = TimeUnit.DAYS,
                        amount = -100,
                        msUntilNextIncrement = TimeUnit.DAYS.toMillis(1)
                    )
                ),
            )
        }

        @JvmStatic
        @Suppress("unused")
        fun testFormat(): List<FormatTestCase> {
            return listOf(
                FormatTestCase(
                    info = RelativeTimeInfo(unit = TimeUnit.SECONDS, amount = 0, msUntilNextIncrement = 0),
                    format = "now"
                ),
                FormatTestCase(
                    info = RelativeTimeInfo(unit = TimeUnit.SECONDS, amount = 1, msUntilNextIncrement = 0),
                    format = "in 1 second"
                ),
                FormatTestCase(
                    info = RelativeTimeInfo(unit = TimeUnit.SECONDS, amount = -1, msUntilNextIncrement = 0),
                    format = "1 second ago"
                ),
                FormatTestCase(
                    info = RelativeTimeInfo(unit = TimeUnit.SECONDS, amount = 2, msUntilNextIncrement = 0),
                    format = "in 2 seconds"
                ),
                FormatTestCase(
                    info = RelativeTimeInfo(unit = TimeUnit.SECONDS, amount = -2, msUntilNextIncrement = 0),
                    format = "2 seconds ago"
                ),
                FormatTestCase(
                    info = RelativeTimeInfo(unit = TimeUnit.MINUTES, amount = 3, msUntilNextIncrement = 0),
                    format = "in 3 minutes"
                ),
                FormatTestCase(
                    info = RelativeTimeInfo(unit = TimeUnit.MINUTES, amount = -3, msUntilNextIncrement = 0),
                    format = "3 minutes ago"
                ),
                FormatTestCase(
                    info = RelativeTimeInfo(unit = TimeUnit.HOURS, amount = 4, msUntilNextIncrement = 0),
                    format = "in 4 hours"
                ),
                FormatTestCase(
                    info = RelativeTimeInfo(unit = TimeUnit.HOURS, amount = -4, msUntilNextIncrement = 0),
                    format = "4 hours ago"
                ),
                FormatTestCase(
                    info = RelativeTimeInfo(unit = TimeUnit.DAYS, amount = 5, msUntilNextIncrement = 0),
                    format = "in 5 days"
                ),
                FormatTestCase(
                    info = RelativeTimeInfo(unit = TimeUnit.DAYS, amount = -5, msUntilNextIncrement = 0),
                    format = "5 days ago"
                ),
            )
        }
    }
}
