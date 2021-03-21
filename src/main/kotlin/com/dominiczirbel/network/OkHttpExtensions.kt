package com.dominiczirbel.network

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
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
 * Reads the [Response.body] of this [Response] as JSON into an object of type [T].
 */
inline fun <reified T> Response.bodyFromJson(): T {
    val body = requireNotNull(body) { "No response body" }

    // workaround: Json deserialization doesn't handle deserializing Unit from an empty string
    if (T::class == Unit::class) {
        body.string().let { bodyString ->
            require(bodyString.isEmpty()) { "Body deserialized to Unit was not empty: $bodyString" }
        }
        return Unit as T
    }

    // may throw NPE if T is not non-nullable
    return body.string().takeIf { it.isNotEmpty() }?.let { bodyString ->
        try {
            Json.decodeFromString<T>(bodyString)
        } catch (ex: SerializationException) {
            // the default message is not very helpful
            throw Throwable(message = "Error deserializing ${T::class} from:\n\n$bodyString\n", cause = ex)
        }
    } as T
}
