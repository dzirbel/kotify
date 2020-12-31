package com.dominiczirbel.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
            }
        )

        continuation.invokeOnCancellation {
            runCatching { cancel() }
        }
    }
}

/**
 * Reads the [Response.body] of this [Response] as JSON into an object of type [T], using the given [gson].
 *
 * Note that [T] must be reified even though it is not enforced by the compiler in order to preserve parameterization.
 */
inline fun <reified T> Response.bodyFromJson(gson: Gson): T {
    val body = requireNotNull(body) { "No response body" }

    // use TypeToken to preserve generics
    return requireNotNull(gson.fromJson(body.charStream(), object : TypeToken<T>() {}.type))
}
