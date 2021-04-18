package com.dzirbel.kotify.cache

/**
 * A generic strategy for determining whether new values should replace existing cached values.
 */
interface CacheReplacementStrategy {
    /**
     * Determines whether the [current] object should be replaced by the [new] value.
     */
    fun replace(current: Any, new: Any): Boolean

    /**
     * A [CacheTTLStrategy] which always replaces cached values.
     */
    object AlwaysReplace : CacheReplacementStrategy {
        override fun replace(current: Any, new: Any) = true
    }

    /**
     * A [CacheTTLStrategy] which never replaces cached values.
     */
    object NeverReplace : CacheReplacementStrategy {
        override fun replace(current: Any, new: Any) = false
    }
}
