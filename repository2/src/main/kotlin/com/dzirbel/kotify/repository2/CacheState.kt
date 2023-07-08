package com.dzirbel.kotify.repository2

import java.time.Instant

/**
 * Wraps the state of a value retrieved from a [Repository].
 */
sealed interface CacheState<T> {
    /**
     * The current value to be reflected.
     */
    val cachedValue: T?
        get() = null

    /**
     * The [Instant] at which the value was last fetched from the remote data source.
     */
    val cacheTime: Instant?
        get() = null

    /**
     * Indicates that the value is being refreshed from the remote data source.
     *
     * Optionally includes a [cachedValue] and [cacheTime] to be displayed while refreshing, i.e. when refreshing but
     * the previous value is still valid.
     */
    data class Refreshing<T>(
        override val cachedValue: T? = null,
        override val cacheTime: Instant? = null,
    ) : CacheState<T>

    /**
     * Indicates that the [cachedValue] is loaded and ready to be displayed.
     */
    data class Loaded<T>(override val cachedValue: T, override val cacheTime: Instant) : CacheState<T>

    /**
     * Indicates that the value could not be found in either the local cache or remote data source.
     */
    class NotFound<T> : CacheState<T>

    /**
     * Indicates that [throwable] was thrown when loading the value.
     */
    data class Error<T>(val throwable: Throwable) : CacheState<T>
}
