package com.dominiczirbel.util

/**
 * A cache which evicts the least-recently-used values when its size exceeds [maxSize] (or never evicts values if
 * [maxSize] is null).
 */
class LruCache<K, V>(
    private val maxSize: Int?,
    loadFactor: Float = 0.75f,
    initialCapacity: Int = 32
) : LinkedHashMap<K, V>(initialCapacity, loadFactor, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>): Boolean {
        return maxSize != null && size > maxSize
    }
}
