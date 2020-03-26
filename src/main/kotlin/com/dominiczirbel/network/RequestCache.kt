package com.dominiczirbel.network

import com.dominiczirbel.util.LruCache
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.ResponseOf
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * A cache for asynchronous HTTP requests for results of type [T] by keys of type [K].
 *
 * Both in-progress requests and completed ones (up to [maxSize], with LRU eviction of older requests) are cached in
 * memory.
 */
class RequestCache<K, T>(maxSize: Int? = 0) {
    private val inProgress = mutableMapOf<K, Deferred<T>>()
    private val complete = LruCache<K, T>(maxSize = maxSize)

    private val totalRequestsAtomic = AtomicInteger(0)

    val totalRequests
        get() = totalRequestsAtomic.get()

    val numInProgress: Int
        @TestOnly
        get() = synchronized(inProgress) { inProgress.size }

    val durations = mutableListOf<Long>()
    val statusCodeCounts = mutableMapOf<Int, Int>()

    @TestOnly
    fun clear() {
        synchronized(inProgress) {
            inProgress.clear()
        }
        totalRequestsAtomic.set(0)
        complete.clear()
        durations.clear()
        statusCodeCounts.clear()
    }

    fun getCached(key: K): T? = complete[key]

    suspend fun request(key: K, suspendedRequest: suspend () -> ResponseOf<T>): T {
        complete[key]?.let { return it }

        return coroutineScope {
            synchronized(inProgress) {
                inProgress.getOrPut(key) {
                    async(context = Dispatchers.IO) {
                        totalRequestsAtomic.incrementAndGet()
                        try {
                            val start = System.nanoTime()
                            val (_, response, value) = try {
                                suspendedRequest()
                            } catch (ex: FuelError) {
                                track(start = start, statusCode = ex.response.statusCode)
                                throw ex
                            }

                            track(start = start, statusCode = response.statusCode)
                            complete[key] = value

                            value
                        } finally {
                            @Suppress("DeferredResultUnused")
                            synchronized(inProgress) {
                                inProgress.remove(key)
                            }
                        }
                    }
                }
            }
        }.await()
    }

    private fun track(start: Long, statusCode: Int) {
        synchronized(this) {
            durations.add(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start))
            if (statusCode in validStatusCodes) {
                statusCodeCounts.compute(statusCode) { _, count -> (count ?: 0) + 1 }
            }
        }
    }

    companion object {
        private val validStatusCodes = 100 until 600
    }
}
