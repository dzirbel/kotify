package com.dzirbel.kotify.repository

import androidx.compose.runtime.Stable
import com.dzirbel.kotify.db.SpotifyEntity
import java.time.Instant

/**
 * Wraps the state of a value retrieved from a [Repository].
 */
@Stable
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
    ) : CacheState<T> {
        companion object {
            /**
             * Creates a [Refreshing] state from the given [cacheState] which preserves its values, e.g. to indicate
             * that a value is being refreshed while still displaying the current state.
             */
            fun <T> of(cacheState: CacheState<T>?): Refreshing<T> {
                return Refreshing(cachedValue = cacheState?.cachedValue, cacheTime = cacheState?.cacheTime)
            }
        }
    }

    /**
     * Indicates that the [cachedValue] is loaded and ready to be displayed.
     */
    data class Loaded<T>(override val cachedValue: T, override val cacheTime: Instant) : CacheState<T> {
        companion object {
            /**
             * Creates a [Loaded] state from the given [SpotifyEntity], using its [SpotifyEntity.updatedTime].
             */
            fun <T : SpotifyEntity> of(value: T): Loaded<T> {
                return Loaded(cachedValue = value, cacheTime = value.updatedTime)
            }

            /**
             * Creates a [Loaded] state from the given [SpotifyEntity] if not null, otherwise a [NotFound].
             */
            fun <T : SpotifyEntity> orNotFound(value: T?): CacheState<T> {
                return value?.let(::of) ?: NotFound()
            }
        }
    }

    /**
     * Indicates that the value could not be found in either the local cache or remote data source.
     */
    class NotFound<T> : CacheState<T>

    /**
     * Indicates that [throwable] was thrown when loading the value.
     */
    data class Error<T>(val throwable: Throwable) : CacheState<T>
}
