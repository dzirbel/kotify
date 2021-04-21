package com.dzirbel.kotify.util

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
    val unit: TimeUnit,
    val amount: Long,
    val msUntilNextIncrement: Long
) {
    /**
     * Returns a user-readable format of this [RelativeTimeInfo].
     */
    fun format(): String {
        if (amount == 0L) return "now"

        val timeUnitName = when (unit) {
            TimeUnit.SECONDS -> "second"
            TimeUnit.MINUTES -> "minute"
            TimeUnit.HOURS -> "hour"
            TimeUnit.DAYS -> "day"
            else -> error("unexpected TimeUnit $unit")
        }

        val unitFormatted = if (abs(amount) == 1L) timeUnitName else timeUnitName + "s"
        val amountFormatted = if (amount == 0L) "<1" else abs(amount).toString()

        return if (amount < 0) "$amountFormatted $unitFormatted ago" else "in $amountFormatted $unitFormatted"
    }

    companion object {
        /**
         * Creates a [RelativeTimeInfo] from the given [timestamp], relative to [now].
         */
        fun of(timestamp: Long, now: Long = System.currentTimeMillis()): RelativeTimeInfo {
            val differenceMs = timestamp - now
            val absDifferenceMs = abs(timestamp - now)
            for (unit in listOf(TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.SECONDS)) {
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
                        }
                    )
                }
            }

            return RelativeTimeInfo(
                unit = TimeUnit.SECONDS,
                amount = 0,
                msUntilNextIncrement = TimeUnit.SECONDS.toMillis(1) - differenceMs
            )
        }
    }
}
