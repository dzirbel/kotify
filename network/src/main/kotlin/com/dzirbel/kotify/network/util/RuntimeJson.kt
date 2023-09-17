package com.dzirbel.kotify.network.util

import com.dzirbel.kotify.Runtime
import kotlinx.serialization.json.Json

/**
 * Creates a [Json] instance for this [Runtime].
 */
val Runtime.json
    get() = Json {
        ignoreUnknownKeys = !debug
        isLenient = !debug
        coerceInputValues = !debug
    }
