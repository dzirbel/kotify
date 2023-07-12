package com.dzirbel.kotify.repository2.util

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Provides a strategy for retrying failed calls.
 */
internal interface BackoffStrategy {
    /**
     * Returns the [Duration] to delay after a failure for the given [attempt], starting with 0 for a failure on the
     * first attempt. Returns null if the operation should be aborted and no retry made.
     */
    fun delayFor(attempt: Int): Duration?

    /**
     * A simple [BackoffStrategy] determined by [delaysMs].
     */
    class OfDelays(private vararg val delaysMs: Long) : BackoffStrategy {
        override fun delayFor(attempt: Int): Duration? {
            return delaysMs.getOrNull(attempt)?.milliseconds
        }
    }

    companion object {
        /**
         * The default [BackoffStrategy] with a fixed number of delays of increasing duration.
         */
        val default = OfDelays(250, 500, 500, 2000, 5000)
    }
}
