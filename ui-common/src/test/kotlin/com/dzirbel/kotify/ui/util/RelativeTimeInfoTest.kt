package com.dzirbel.kotify.ui.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.dzirbel.kotify.util.CurrentTime
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.concurrent.TimeUnit

internal class RelativeTimeInfoTest {
    data class RelativeTestCase(val timestamp: Long, val now: Long, val expected: RelativeTimeInfo)
    data class FormatTestCase(val info: RelativeTimeInfo, val formatLong: String, val formatShort: String)

    @ParameterizedTest
    @MethodSource
    fun testRelative(case: RelativeTestCase) {
        CurrentTime.mocked(case.now) {
            val info = RelativeTimeInfo.of(timestamp = case.timestamp)
            assertThat(info).isEqualTo(case.expected)
        }
    }

    @ParameterizedTest
    @MethodSource
    fun testFormat(case: FormatTestCase) {
        assertThat(case.info.formatLong()).isEqualTo(case.formatLong)
        assertThat(case.info.formatShort()).isEqualTo(case.formatShort)
    }

    companion object {
        @JvmStatic
        fun testRelative(): List<RelativeTestCase> {
            val base = 1_600_000_000_000
            return listOf(
                RelativeTestCase(
                    timestamp = base,
                    now = base,
                    expected = RelativeTimeInfo(unit = TimeUnit.SECONDS, amount = 0, msUntilNextIncrement = 1_000),
                ),
                RelativeTestCase(
                    timestamp = base + 1,
                    now = base,
                    expected = RelativeTimeInfo(unit = TimeUnit.SECONDS, amount = 0, msUntilNextIncrement = 999),
                ),
                RelativeTestCase(
                    timestamp = base - 1,
                    now = base,
                    expected = RelativeTimeInfo(unit = TimeUnit.SECONDS, amount = 0, msUntilNextIncrement = 1001),
                ),
                RelativeTestCase(
                    timestamp = base + 1001,
                    now = base,
                    expected = RelativeTimeInfo(unit = TimeUnit.SECONDS, amount = 1, msUntilNextIncrement = 999),
                ),
                RelativeTestCase(
                    timestamp = base - 1001,
                    now = base,
                    expected = RelativeTimeInfo(unit = TimeUnit.SECONDS, amount = -1, msUntilNextIncrement = 1),
                ),
                RelativeTestCase(
                    timestamp = base + 3_600,
                    now = base + 2_000,
                    expected = RelativeTimeInfo(unit = TimeUnit.SECONDS, amount = 1, msUntilNextIncrement = 400),
                ),
                RelativeTestCase(
                    timestamp = base + TimeUnit.MINUTES.toMillis(3) + TimeUnit.SECONDS.toMillis(20),
                    now = base,
                    expected = RelativeTimeInfo(
                        unit = TimeUnit.MINUTES,
                        amount = 3,
                        msUntilNextIncrement = TimeUnit.SECONDS.toMillis(40),
                    ),
                ),
                RelativeTestCase(
                    timestamp = base,
                    now = base + TimeUnit.MINUTES.toMillis(3) + TimeUnit.SECONDS.toMillis(20),
                    expected = RelativeTimeInfo(
                        unit = TimeUnit.MINUTES,
                        amount = -3,
                        msUntilNextIncrement = TimeUnit.SECONDS.toMillis(20),
                    ),
                ),
                RelativeTestCase(
                    timestamp = base + TimeUnit.DAYS.toMillis(100),
                    now = base,
                    expected = RelativeTimeInfo(
                        unit = TimeUnit.DAYS,
                        amount = 100,
                        msUntilNextIncrement = TimeUnit.DAYS.toMillis(1),
                    ),
                ),
                RelativeTestCase(
                    timestamp = base - TimeUnit.DAYS.toMillis(100),
                    now = base,
                    expected = RelativeTimeInfo(
                        unit = TimeUnit.DAYS,
                        amount = -100,
                        msUntilNextIncrement = TimeUnit.DAYS.toMillis(1),
                    ),
                ),
            )
        }

        @JvmStatic
        fun testFormat(): List<FormatTestCase> {
            return listOf(
                FormatTestCase(
                    info = RelativeTimeInfo(unit = TimeUnit.SECONDS, amount = 0, msUntilNextIncrement = 0),
                    formatLong = "now",
                    formatShort = "now",
                ),
                FormatTestCase(
                    info = RelativeTimeInfo(unit = TimeUnit.SECONDS, amount = 1, msUntilNextIncrement = 0),
                    formatLong = "in 1 second",
                    formatShort = "1s",
                ),
                FormatTestCase(
                    info = RelativeTimeInfo(unit = TimeUnit.SECONDS, amount = -1, msUntilNextIncrement = 0),
                    formatLong = "1 second ago",
                    formatShort = "1s",
                ),
                FormatTestCase(
                    info = RelativeTimeInfo(unit = TimeUnit.SECONDS, amount = 2, msUntilNextIncrement = 0),
                    formatLong = "in 2 seconds",
                    formatShort = "2s",
                ),
                FormatTestCase(
                    info = RelativeTimeInfo(unit = TimeUnit.SECONDS, amount = -2, msUntilNextIncrement = 0),
                    formatLong = "2 seconds ago",
                    formatShort = "2s",
                ),
                FormatTestCase(
                    info = RelativeTimeInfo(unit = TimeUnit.MINUTES, amount = 3, msUntilNextIncrement = 0),
                    formatLong = "in 3 minutes",
                    formatShort = "3m",
                ),
                FormatTestCase(
                    info = RelativeTimeInfo(unit = TimeUnit.MINUTES, amount = -3, msUntilNextIncrement = 0),
                    formatLong = "3 minutes ago",
                    formatShort = "3m",
                ),
                FormatTestCase(
                    info = RelativeTimeInfo(unit = TimeUnit.HOURS, amount = 4, msUntilNextIncrement = 0),
                    formatLong = "in 4 hours",
                    formatShort = "4h",
                ),
                FormatTestCase(
                    info = RelativeTimeInfo(unit = TimeUnit.HOURS, amount = -4, msUntilNextIncrement = 0),
                    formatLong = "4 hours ago",
                    formatShort = "4h",
                ),
                FormatTestCase(
                    info = RelativeTimeInfo(unit = TimeUnit.DAYS, amount = 5, msUntilNextIncrement = 0),
                    formatLong = "in 5 days",
                    formatShort = "5d",
                ),
                FormatTestCase(
                    info = RelativeTimeInfo(unit = TimeUnit.DAYS, amount = -5, msUntilNextIncrement = 0),
                    formatLong = "5 days ago",
                    formatShort = "5d",
                ),
            )
        }
    }
}
