package com.dominiczirbel.util

import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class LruCacheTest {
    @Test
    fun `put and clear`() {
        val cache = LruCache<String, String>(maxSize = 3, initialCapacity = 1)

        assertThat(cache).isEmpty()
        cache.putAll("1" to "a", "2" to "b", "3" to "c", "4" to "d", "5" to "e")
        assertThat(cache).hasValues("3" to "c", "4" to "d", "5" to "e")
        cache.clear()
        assertThat(cache).isEmpty("1", "2")
    }

    @Test
    fun `put overwrites existing values`() {
        val cache = LruCache<String, String>(maxSize = 3, initialCapacity = 1)

        assertThat(cache).isEmpty()
        cache.putAll("1" to "a", "2" to "b", "3" to "c")
        assertThat(cache["1"]).isEqualTo("a")
        cache.putAll("4" to "d", "5" to "e")
        assertThat(cache).hasValues("1" to "a", "4" to "d", "5" to "e")
        cache.clear()
        assertThat(cache).isEmpty("1", "2")
    }

    @Test
    fun `zero limit caches nothing`() {
        val cache = LruCache<String, String>(maxSize = 0, initialCapacity = 1)

        assertThat(cache).isEmpty()
        cache["key"] = "value"
        assertThat(cache).isEmpty("key")
    }

    @Test
    fun `no limit caches everything`() {
        val cache = LruCache<String, String>(maxSize = null, initialCapacity = 1)

        assertThat(cache).isEmpty()
        cache.putAll("1" to "a", "2" to "b", "3" to "c", "4" to "d", "5" to "e")
        assertThat(cache).hasValues("1" to "a", "2" to "b", "3" to "c", "4" to "d", "5" to "e")
    }

    private fun <K, V> LruCache<K, V>.putAll(vararg entries: Pair<K, V>) {
        entries.forEach { (key, value) ->
            set(key, value)
        }
    }

    private fun <K, V> assertThat(actual: LruCache<K, V>): LruCacheSubject<K, V> {
        return assertAbout { metadata, factoryActual: LruCache<K, V> -> LruCacheSubject(metadata, factoryActual) }
            .that(actual)
    }

    private class LruCacheSubject<K, V>(failureMetadata: FailureMetadata, private val actual: LruCache<K, V>) :
        Subject(failureMetadata, actual) {

        fun isEmpty(vararg testKeys: K) {
            check("size").that(actual.size).isEqualTo(0)
            check("isEmpty()").that(actual.isEmpty()).isTrue()
            testKeys.forEach { key ->
                check("get($key)").that(actual[key]).isNull()
                check("remove($key)").that(actual.remove(key)).isNull()
            }
        }

        fun hasValues(vararg values: Pair<K, V>) {
            check("size").that(actual.size).isEqualTo(values.size)
            check("isEmpty()").that(actual.isEmpty()).isEqualTo(values.isEmpty())
            values.forEach { (key, value) ->
                check("get($key)").that(actual[key]).isEqualTo(value)
            }
        }
    }
}
