package com.dzirbel.kotify.network

import com.dzirbel.kotify.network.DelayInterceptor.delayMs
import okhttp3.Interceptor
import okhttp3.Response

/**
 * A simple [Interceptor] which adds [delayMs] delay to each request.
 */
object DelayInterceptor : Interceptor {
    var delayMs: Long = 0L

    override fun intercept(chain: Interceptor.Chain): Response {
        if (delayMs > 0) {
            Thread.sleep(delayMs)
        }

        return chain.proceed(chain.request())
    }
}
