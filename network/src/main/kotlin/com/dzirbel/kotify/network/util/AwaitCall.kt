package com.dzirbel.kotify.network.util

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Invokes this [Call], suspending until it returns a [Response].
 *
 * Inspired by https://github.com/gildor/kotlin-coroutines-okhttp
 */
suspend fun Call.await(): Response {
    return suspendCancellableCoroutine { continuation ->
        enqueue(
            object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response)
                }

                override fun onFailure(call: Call, e: IOException) {
                    if (!continuation.isCancelled) {
                        continuation.resumeWithException(e)
                    }
                }
            },
        )

        continuation.invokeOnCancellation {
            runCatching { cancel() }
        }
    }
}
