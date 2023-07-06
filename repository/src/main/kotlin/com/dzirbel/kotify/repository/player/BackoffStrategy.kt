package com.dzirbel.kotify.repository.player

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

// TODO document
// TODO test
internal interface BackoffStrategy {
    fun delayFor(attempt: Int): Duration?

    class OfDelays(private vararg val delays: Long) : BackoffStrategy {
        override fun delayFor(attempt: Int): Duration? {
            return delays.getOrNull(attempt)?.milliseconds
        }
    }

    companion object {
        val default = OfDelays(250, 500, 500, 2000, 5000)
    }
}
