package com.dominiczirbel.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class LruCacheTest {
    @Test
    fun test1() {
        val cache = LruCache<String, String>(maxSize = 3, initialCapacity = 1)

        cache.assertEmpty()
        cache.putAll("1" to "a", "2" to "b", "3" to "c", "4" to "d", "5" to "e")
        cache.assertValues("3" to "c", "4" to "d", "5" to "e")
        cache.clear()
        cache.assertEmpty("1", "2")
    }

    @Test
    fun test2() {
        val cache = LruCache<String, String>(maxSize = 3, initialCapacity = 1)

        cache.assertEmpty()
        cache.putAll("1" to "a", "2" to "b", "3" to "c")
        cache["1"]
        cache.putAll("4" to "d", "5" to "e")
        cache.assertValues("1" to "a", "4" to "d", "5" to "e")
        cache.clear()
        cache.assertEmpty("1", "2")
    }

    @Test
    fun testZeroLimit() {
        val cache = LruCache<String, String>(maxSize = 0, initialCapacity = 1)

        cache.assertEmpty()
        cache["key"] = "value"
        cache.assertEmpty("key")
    }

    @Test
    fun testNoLimit() {
        val cache = LruCache<String, String>(maxSize = null, initialCapacity = 1)

        cache.assertEmpty()
        cache.putAll("1" to "a", "2" to "b", "3" to "c", "4" to "d", "5" to "e")
        cache.assertValues("1" to "a", "2" to "b", "3" to "c", "4" to "d", "5" to "e")
    }

    private fun <K, V> LruCache<K, V>.putAll(vararg entries: Pair<K, V>) {
        entries.forEach { (key, value) ->
            set(key, value)
        }
    }

    private fun <K, V> LruCache<K, V>.assertEmpty(vararg testKeys: K) {
        assertEquals(0, size)
        assertTrue(isEmpty())
        testKeys.forEach { key ->
            assertNull(get(key))
            assertNull(remove(key))
        }
    }

    private fun <K, V> LruCache<K, V>.assertValues(vararg values: Pair<K, V>) {
        assertEquals(values.size, size)
        assertEquals(size == 0, isEmpty())
        values.forEach { (key, value) ->
            assertEquals(value, get(key))
        }
    }
}
