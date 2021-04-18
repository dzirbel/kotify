package com.dzirbel.kotify.cache

/**
 * An object which can be added to a [Cache] which provides its own [id] and an optional set of additional
 * [cacheableObjects] which are associated with it and should also be cached.
 *
 * This allows for recursive addition of objects attached to a single object, for example if fetching an album also
 * returns a list of its tracks, the track objects can be individually cached as well.
 *
 * TODO don't save associated objects as part of the cache? (but we need to re-associate them when loading)
 */
interface CacheableObject {
    /**
     * The ID of the cached object; if null it will not be cached.
     */
    val id: String?

    /**
     * An optional collection of associated objects which should also be cached alongside this [CacheableObject].
     *
     * Should NOT include this [CacheableObject].
     */
    val cacheableObjects: Collection<CacheableObject>
        get() = emptySet()

    /**
     * Recursively finds all associated [CacheableObject] from this [CacheableObject] and its [cacheableObjects].
     *
     * Note that this will loop infinitely if there is a cycle of [CacheableObject]s, so associations must be acyclic.
     */
    val recursiveCacheableObjects: Collection<CacheableObject>
        get() = cacheableObjects.flatMap { it.recursiveCacheableObjects }.plus(this)
}
