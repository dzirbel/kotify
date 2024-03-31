package com.dzirbel.kotify.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration

/**
 * Runs [block] up to [attempts] times, returning the first value for which it did not throw an exception.
 *
 * If all attempts throw an exception, throw a wrapper around the last thrown one. Also optionally delays by the given
 * [delayBetweenAttempts] duration between attempts.
 */
fun <T> retryForResult(attempts: Int, delayBetweenAttempts: Duration? = null, block: () -> T): T {
    lateinit var throwable: Throwable
    repeat(attempts) { attempt ->
        try {
            return block()
        } catch (t: Throwable) {
            throwable = t
        }

        // do not delay after the last attempt
        if (attempt != attempts - 1 && delayBetweenAttempts != null) {
            runBlocking { delay(delayBetweenAttempts) }
        }
    }

    throw Throwable("Failed after $attempts attempts", cause = throwable)
}
