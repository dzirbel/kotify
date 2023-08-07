package com.dzirbel.kotify.repository

import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

/**
 * A simple mechanism to determine whether locally cached values are still valid.
 */
fun interface CacheStrategy<T> {
    /**
     * Captures the state of a value in the cache; in particular both whether it can be displayed and/or whether a new
     * value should be fetched from the remote data source.
     */
    enum class CacheValidity(val canBeUsed: Boolean, val shouldBeRefreshed: Boolean) {
        /**
         * Indicates the cached value is valid and can be used.
         */
        VALID(canBeUsed = true, shouldBeRefreshed = false),

        /**
         * Indicates the cached value is invalid and should not be used, even while being refreshed.
         */
        INVALID(canBeUsed = false, shouldBeRefreshed = true),

        /**
         * Indicates the cached value is invalid but can be used while being refreshed.
         */
        TRANSIENT(canBeUsed = true, shouldBeRefreshed = true),
    }

    /**
     * Determines the [CacheValidity] for the given [value].
     */
    fun validity(value: T): CacheValidity

    /**
     * Chains this [CacheStrategy] with the given one, taking the more restrictive [CacheValidity].
     */
    fun then(other: CacheStrategy<T>): CacheStrategy<T> {
        return CacheStrategy { value ->
            when (val validity = validity(value)) {
                CacheValidity.INVALID -> validity
                CacheValidity.TRANSIENT -> other.validity(value).takeIf { it == CacheValidity.INVALID } ?: validity
                CacheValidity.VALID -> other.validity(value)
            }
        }
    }

    /**
     * Marks all cached entries as invalid, forcing them to be fetched from the remote data source.
     *
     * Typically used when manually refreshing a cached value.
     */
    class NeverValid<T> : CacheStrategy<T> {
        override fun validity(value: T) = CacheValidity.INVALID
    }

    /**
     * Marks all cached entries as valid, always using locally cached data when it is present.
     */
    class AlwaysValid<T> : CacheStrategy<T> {
        override fun validity(value: T) = CacheValidity.VALID
    }

    /**
     * A [CacheStrategy] which applies the given TTLs to the cached value.
     *
     * In particular, if the update time as provided by [getUpdateTime] is older than [invalidTTL], the value will
     * be considered [CacheValidity.INVALID]; if older than [transientTTL], the value will be considered
     * [CacheValidity.TRANSIENT]; otherwise [CacheValidity.VALID]. If [transientTTL] or [invalidTTL] are null, they will
     * be ignored.
     */
    class TTL<T>(
        private val transientTTL: Duration? = defaultTransientDuration,
        private val invalidTTL: Duration? = defaultInvalidDuration,
        private val getUpdateTime: (T) -> Instant,
    ) : CacheStrategy<T> {
        override fun validity(value: T): CacheValidity {
            val updateTime = getUpdateTime(value)
            val now = Instant.now()

            return when {
                invalidTTL != null && updateTime.plus(invalidTTL.toJavaDuration()) < now -> CacheValidity.INVALID
                transientTTL != null && updateTime.plus(transientTTL.toJavaDuration()) < now -> CacheValidity.TRANSIENT
                else -> CacheValidity.VALID
            }
        }

        companion object {
            val defaultTransientDuration = 7.days
            val defaultInvalidDuration = 120.days
        }
    }

    /**
     * A [CacheStrategy] for an [EntityViewModel] which applies the given TTLs to the cached value, based on the
     * [EntityViewModel.updatedTime].
     */
    class EntityTTL<T : EntityViewModel>(
        refreshTTL: Duration? = TTL.defaultTransientDuration,
        invalidTTL: Duration? = TTL.defaultInvalidDuration,
    ) : CacheStrategy<T> by TTL(transientTTL = refreshTTL, invalidTTL = invalidTTL, getUpdateTime = { it.updatedTime })

    /**
     * A [CacheStrategy] for an [EntityViewModel] which requires that the value has an
     * [EntityViewModel.fullUpdatedTime].
     */
    class RequiringFullEntity<T : EntityViewModel>(
        private val otherwise: CacheValidity = CacheValidity.TRANSIENT,
    ) : CacheStrategy<T> {
        override fun validity(value: T): CacheValidity {
            return if (value.fullUpdatedTime != null) CacheValidity.VALID else otherwise
        }
    }
}
