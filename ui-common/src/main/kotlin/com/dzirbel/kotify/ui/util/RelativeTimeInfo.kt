package com.dzirbel.kotify.ui.util

import com.dzirbel.kotify.util.CurrentTime
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Represents a relative duration as an [amount] (which may be negative) and a [unit].
 *
 * This relative duration is meant to be user-friendly, simply truncated to the largest [TimeUnit] of the duration and
 * its [amount]. A user-readable representation can be obtained via [formatLong].
 *
 * Also includes the amount of time until the [amount] would change as [msUntilNextIncrement].
 */
data class RelativeTimeInfo(
    internal val unit: TimeUnit,
    internal val amount: Long,
    internal val msUntilNextIncrement: Long,
) {
    /**
     * Returns a user-readable format of this [RelativeTimeInfo] in the form of "X seconds ago" or "in X minutes".
     */
    fun formatLong(): String {
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

    /**
     * Returns a user-readable format of this [RelativeTimeInfo] in the form of "Xs" or "Xm".
     */
    fun formatShort(): String {
        if (amount == 0L) return "now"

        val timeUnitName = when (unit) {
            TimeUnit.SECONDS -> "s"
            TimeUnit.MINUTES -> "m"
            TimeUnit.HOURS -> "h"
            TimeUnit.DAYS -> "d"
            else -> error("unexpected TimeUnit $unit")
        }

        return "${abs(amount)}$timeUnitName"
    }

    companion object {
        /**
         * Creates a [RelativeTimeInfo] from the given [timestamp].
         */
        internal fun of(timestamp: Long): RelativeTimeInfo {
            val now: Long = CurrentTime.millis
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
