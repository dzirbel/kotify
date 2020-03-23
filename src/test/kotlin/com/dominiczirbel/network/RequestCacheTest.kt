package com.dominiczirbel.network

import com.dominiczirbel.assertThrowsInline
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.ResponseOf
import com.github.kittinunf.fuel.core.requests.DefaultRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.RepetitionInfo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

internal class RequestCacheTest {
    private suspend fun <K, T : Any> RequestCache<K, T>.request(
        key: K,
        result: T,
        url: String = "http://example.com",
        method: Method = Method.GET,
        callback: (suspend () -> Unit)? = null
    ): T {
        @Suppress("BlockingMethodInNonBlockingContext")
        val urlUrl = URL(url)
        return request(key) {
            callback?.invoke()
            ResponseOf(DefaultRequest(method = method, url = urlUrl), Response(url = urlUrl), result)
        }
    }

    private fun RequestCache<*, *>.assertRequests(total: Int, inProgress: Int = 0) {
        assertEquals(total, totalRequests) { "expected $total total, was $totalRequests" }
        assertEquals(inProgress, numInProgress) { "expected $inProgress in progress, was $numInProgress" }
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 3, 10, 25])
    fun testSuccessUnbounded(n: Int) {
        val cache = RequestCache<String, Int>(maxSize = null)

        runBlocking {
            for (i in 1..n) {
                assertEquals(i * 2, cache.request("key$i", i * 2))
            }
            cache.assertRequests(total = n)

            // check that we still have cached values i*2
            for (i in 1..n) {
                assertEquals(i * 2, cache.request("key$i", i * 3))
            }
            cache.assertRequests(total = n)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 3, 10, 25])
    fun testSuccessNoCache(n: Int) {
        val cache = RequestCache<String, Int>(maxSize = 0)

        runBlocking {
            for (i in 1..n) {
                assertEquals(i * 2, cache.request("key$i", i * 2))
            }
            cache.assertRequests(total = n)

            // check that the values i*2 were not cached
            for (i in 1..n) {
                assertEquals(i * 3, cache.request("key$i", i * 3))
            }
            cache.assertRequests(total = 2 * n)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 3, 10, 25])
    fun testSuccessCache(n: Int) {
        val cache = RequestCache<String, Int>(maxSize = n / 2)

        runBlocking {
            for (i in 1..n) {
                assertEquals(i * 2, cache.request("key$i", i * 2))
            }
            cache.assertRequests(total = n)

            // since we start back from 1, none of the values are cached any more
            for (i in 1..n) {
                assertEquals(i * 3, cache.request("key$i", i * 3))
            }
            cache.assertRequests(total = 2 * n)

            // when we count down from n, the first n/2 keys are still cached as i*3, the rest are computed as i*4
            for ((count, i) in (n downTo 1).withIndex()) {
                if (count < n / 2) {
                    assertEquals(i * 3, cache.request("key$i", i * 4))
                } else {
                    assertEquals(i * 4, cache.request("key$i", i * 4))
                }
            }
            cache.assertRequests(total = (2.5 * n).roundToInt())
        }
    }

    @Test
    fun testError() {
        val cache = RequestCache<String, Int>()

        runBlocking {
            assertThrowsInline<FuelError> {
                cache.request("key") {
                    throw FuelError.wrap(Throwable("test error"))
                }
            }
            cache.assertRequests(total = 1)
        }
    }

    @RepeatedTest(REPETITIONS)
    fun testConcurrentSameKey() {
        val cache = RequestCache<String, Int>(maxSize = null)
        val calls = AtomicInteger(0)
        val delayMs = 50L
        val key = "key"
        val value = 1

        runBlocking {
            val async1 = async {
                cache.request(key, value) {
                    delay(delayMs)
                    calls.incrementAndGet()
                }
            }
            val async2 = async {
                cache.request(key, value) {
                    delay(delayMs)
                    calls.incrementAndGet()
                }
            }

            assertEquals(value, async1.await())
            assertEquals(value, async2.await())
            assertEquals(1, calls.get())
            cache.assertRequests(total = 1)

            assertEquals(value, withTimeout(5) { cache.request(key, 0) })
            cache.assertRequests(total = 1)
        }
    }

    @ExperimentalTime
    @RepeatedTest(REPETITIONS)
    fun testConcurrentDifferentKey(repetitionInfo: RepetitionInfo) {
        val cache = RequestCache<String, Int>(maxSize = null)
        val calls = AtomicInteger(0)
        val delayMs = 50L
        val key1 = "key1"
        val value1 = 1
        val key2 = "key2"
        val value2 = 2

        runBlocking {
            lateinit var coroutine1: CoroutineScope
            lateinit var coroutine2: CoroutineScope

            val async1 = async {
                cache.request(key1, value1) {
                    coroutine1 = this
                    delay(delayMs)
                    calls.incrementAndGet()
                }
            }
            val async2 = async {
                cache.request(key2, value2) {
                    coroutine2 = this
                    delay(delayMs)
                    calls.incrementAndGet()
                }
            }

            val mark = TimeSource.Monotonic.markNow()
            val result1 = async1.await()
            val result2 = async2.await()
            val durationMs = mark.elapsedNow().inMilliseconds.roundToLong()

            // only test timeout after the first repetition to allow the JVM to warm up
            if (repetitionInfo.currentRepetition > 1) {
                val maxDelay = (delayMs * 1.75).roundToLong()
                assertTrue(durationMs in delayMs..maxDelay) { "took ${durationMs}ms" }
            }

            assertEquals(value1, result1)
            assertEquals(value2, result2)
            assertNotEquals(coroutine1, coroutine2)
            assertEquals(2, calls.get())
            cache.assertRequests(total = 2)

            assertEquals(value1, withTimeout(5) { cache.request(key1, 0) })
            assertEquals(value2, withTimeout(5) { cache.request(key2, 0) })
            cache.assertRequests(total = 2)
        }
    }

    companion object {
        private const val REPETITIONS = 5
    }
}
