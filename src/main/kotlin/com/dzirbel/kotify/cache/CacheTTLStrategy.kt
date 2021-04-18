package com.dzirbel.kotify.cache

import kotlin.reflect.KClass

/**
 * A generic strategy for determining whether cached values are still valid; typically cached values may become
 * invalid after a certain amount of time in the cache.
 */
interface CacheTTLStrategy {
    fun isValid(cacheObject: CacheObject, currentTime: Long): Boolean {
        return isValid(cacheTime = cacheObject.cacheTime, currentTime = currentTime, obj = cacheObject.obj)
    }

    /**
     * Determines whether the given [obj], cached at [cacheTime], is still valid.
     */
    fun isValid(cacheTime: Long, currentTime: Long, obj: Any): Boolean

    /**
     * A [CacheTTLStrategy] which always marks cache elements as valid, so they will never be evicted.
     */
    object AlwaysValid : CacheTTLStrategy {
        override fun isValid(cacheTime: Long, currentTime: Long, obj: Any) = true
    }

    /**
     * A [CacheTTLStrategy] which never marks cache elements as valid, so they will always be fetched from a remote
     * source. This is equivalent to not having a cache.
     */
    object NeverValid : CacheTTLStrategy {
        override fun isValid(cacheTime: Long, currentTime: Long, obj: Any) = false
    }

    /**
     * A [CacheTTLStrategy] with a [ttl] applied to all cache elements; elements in teh cache for more that [ttl]
     * milliseconds will be evicted.
     */
    class UniversalTTL(private val ttl: Long) : CacheTTLStrategy {
        override fun isValid(cacheTime: Long, currentTime: Long, obj: Any): Boolean {
            return cacheTime + ttl >= currentTime
        }
    }

    /**
     * A [CacheTTLStrategy] with a TTL applied class-by-class to cache elements, so elements of certain classes may
     * persist longer in the cache than elements of other classes.
     *
     * [defaultTTL] is used for elements of classes that are not in [classMap]; if null and a cached element is not
     * in [classMap], an exception will be thrown.
     */
    class TTLByClass(
        private val classMap: Map<KClass<*>, Long>,
        private val defaultTTL: Long? = null
    ) : CacheTTLStrategy {
        override fun isValid(cacheTime: Long, currentTime: Long, obj: Any): Boolean {
            val ttl = classMap[obj::class] ?: defaultTTL
            requireNotNull(ttl) { "no TTL for class ${obj::class}" }

            return cacheTime + ttl >= currentTime
        }
    }
}
