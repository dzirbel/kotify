package com.dzirbel.kotify.util.immutable

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

/**
 * Returns a [PersistentMap] from the results of [map] to the number of times they occur.
 */
fun <T, K> Iterable<T>.countBy(map: (T) -> K): PersistentMap<K, Int> {
    val counts = persistentMapOf<K, Int>().builder()
    for (element in this) {
        counts.compute(map(element)) { _, count -> if (count == null) 1 else count + 1 }
    }
    return counts.build()
}
