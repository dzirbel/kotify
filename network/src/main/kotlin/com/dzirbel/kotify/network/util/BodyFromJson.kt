package com.dzirbel.kotify.network.util

import com.dzirbel.kotify.Runtime
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.Response

/**
 * Reads the [Response.body] of this [Response] as JSON into an object of type [T].
 */
inline fun <reified T> Response.bodyFromJson(json: Json = Runtime.json): T {
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
            json.decodeFromString<T>(bodyString)
        } catch (ex: SerializationException) {
            // the default message is not very helpful
            throw SerializationException(
                message = """
                    Error deserializing ${T::class} from:

                    $bodyString
                """.trimIndent(),
                cause = ex,
            )
        }
    } as T
}
