package com.dzirbel.kotify.util

/**
 * Returns a typesafe copy of this [Map] with its null values removed.
 */
fun <K, V : Any> Map<K, V?>.filterNotNullValues(): Map<K, V> {
    @Suppress("UNCHECKED_CAST")
    return filterValues { it != null } as Map<K, V>
}
