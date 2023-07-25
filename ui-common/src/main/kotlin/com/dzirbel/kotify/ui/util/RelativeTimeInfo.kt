package com.dzirbel.kotify.ui.util

import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Represents a relative duration as an [amount] (which may be negative) and a [unit].
 *
 * This relative duration is meant to be user-friendly, simply truncated to the largest [TimeUnit] of the duration and
 * its [amount]. A user-readable representation can be obtained via [format].
 *
 * Also includes the amount of time until the [amount] would change as [msUntilNextIncrement].
 */
data class RelativeTimeInfo(
    internal val unit: TimeUnit,
    internal val amount: Long,
    internal val msUntilNextIncrement: Long,
) {
    /**
     * Returns a user-readable format of this [RelativeTimeInfo].
     */
    internal fun format(): String {
        if (amount == 0L) return "now"

        val timeUnitName = when (unit) {
            TimeUnit.SECONDS -> "second"
            TimeUnit.MINUTES -> "minute"
            TimeUnit.HOURS -> "hour"
            TimeUnit.DAYS -> "day"
            else -> error("unexpected TimeUnit $unit")
        }

        val absAmount = abs(amount)
        val unitFormatted = if (absAmount == 1L) timeUnitName else timeUnitName + "s"

        return if (amount < 0) "$absAmount $unitFormatted ago" else "in $absAmount $unitFormatted"
    }

    companion object {
        private var mockedTime: Instant? = null

        /**
         * Uses [time] as the current time for computing relative time [of] within [block]. Used for mocking how
         * relative time appears in tests.
         */
        fun withMockedTime(time: Instant = Instant.now(), block: (Instant) -> Unit) {
            require(mockedTime == null)
            mockedTime = time
            try {
                block(time)
            } finally {
                mockedTime = null
            }
        }

        /**
         * Creates a [RelativeTimeInfo] from the given [timestamp], relative to the current time (or [withMockedTime]).
         */
        internal fun of(timestamp: Long): RelativeTimeInfo {
            val now: Long = mockedTime?.toEpochMilli() ?: System.currentTimeMillis()
            val differenceMs = timestamp - now
            val absDifferenceMs = abs(timestamp - now)
            for (unit in listOf(TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.SECONDS)) {
                @Suppress("KDocReferencesNonPublicProperty") // detekt false positive
                val amount = unit.convert(absDifferenceMs, TimeUnit.MILLISECONDS)
                if (amount > 0) {
                    val unitMillis = unit.toMillis(1)
                    return RelativeTimeInfo(
                        unit = unit,
                        amount = if (timestamp > now) amount else -amount,
                        msUntilNextIncrement = if (differenceMs > 0) {
                            unitMillis - differenceMs % unitMillis
                        } else {
                            (absDifferenceMs % unitMillis).takeIf { it != 0L } ?: unitMillis
                        },
                    )
                }
            }

            return RelativeTimeInfo(
                unit = TimeUnit.SECONDS,
                amount = 0,
                msUntilNextIncrement = TimeUnit.SECONDS.toMillis(1) - differenceMs,
            )
        }
    }
}
