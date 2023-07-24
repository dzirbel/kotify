package com.dzirbel.kotify.repository

import com.dzirbel.kotify.db.SpotifyEntity
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * A simple mechanism to determine whether locally cached values are still valid.
 */
fun interface CacheStrategy<T> {
    fun isValid(value: T): Boolean

    /**
     * Marks all cached entries as invalid, forcing them to be fetched from the remote data source.
     */
    class NeverValid<T> : CacheStrategy<T> {
        override fun isValid(value: T) = false
    }

    /**
     * Marks all cached entries as valid, always using locally cached data when it is present.
     */
    class AlwaysValid<T> : CacheStrategy<T> {
        override fun isValid(value: T) = true
    }

    /**
     * Marks cached entries as valid only if they have been refreshed within the given [ttl] duration.
     *
     * Note that this uses [SpotifyEntity.updatedTime] rather than [SpotifyEntity.fullUpdatedTime] so some values may
     */
    class TTL<T : SpotifyEntity>(
        private val ttl: Duration = defaultDuration,
        private val requireFullUpdate: Boolean = false,
    ) : CacheStrategy<T> {
        override fun isValid(value: T): Boolean {
            val updateTime = (if (requireFullUpdate) value.fullUpdatedTime else value.updatedTime)
                ?: return false

            // valid if: updateTime + ttl >= now
            return updateTime.plusMillis(ttl.inWholeMilliseconds).isAfter(Instant.now())
        }

        companion object {
            val defaultDuration = 30.days
        }
    }
}
