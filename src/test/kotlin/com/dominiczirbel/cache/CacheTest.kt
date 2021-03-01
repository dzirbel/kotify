package com.dominiczirbel.cache

import com.dominiczirbel.network.model.SpotifyObject
import com.google.common.collect.Range
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File

internal class CacheTest {
    @Serializable
    private data class SimpleObject(
        override val id: String,
        override val name: String,
        override val href: String? = null,
        override val type: String = "simple",
        override val uri: String? = null
    ) : SpotifyObject

    @Serializable
    private data class SimpleObject2(
        override val id: String,
        override val name: String,
        override val href: String? = null,
        override val type: String = "simple",
        override val uri: String? = null
    ) : SpotifyObject

    private val cache = Cache(testCacheFile)

    @Test
    fun testEmpty() {
        assertThat(cache.getCached("id")).isNull()
        assertThat(cache.cached).isEmpty()
        assertThat(cache.allOfType<SimpleObject>()).isEmpty()
        assertThat(cache.allOfType<Any>()).isEmpty()
    }

    @Test
    fun testPutAndGet() {
        val obj = SimpleObject(id = "id", name = "object")

        assertThat(cache.getCached("id")).isNull()
        cache.put("id", obj)

        val cachedObject = requireNotNull(cache.getCached("id"))
        assertThat(cachedObject.obj).isSameInstanceAs(obj)
        assertThat(cachedObject.id).isEqualTo(obj.id)
        assertThat(cachedObject.cacheTime)
            .isIn(Range.open(System.currentTimeMillis() - 5, System.currentTimeMillis() + 5))
    }

    @Test
    fun testInvalidate() {
        val obj = SimpleObject(id = "id", name = "object")

        cache.put("id", obj)

        cache.assertContains(obj)

        assertThat(cache.invalidate("id")?.obj).isEqualTo(obj)

        assertThat(cache.getCached("id")).isNull()
    }

    @Test
    fun testRemoteCalls() {
        var calls = 0
        val obj1 = SimpleObject(id = "id1", name = "obj1")
        val obj2 = SimpleObject(id = "id2", name = "obj2")
        fun getObj1() = obj1.also { calls++ }
        fun getObj2() = obj2.also { calls++ }

        assertThat(calls).isEqualTo(0)
        assertThat(cache.allOfType<SimpleObject>()).isEmpty()

        assertThat(cache.get("id1", ::getObj1)).isEqualTo(obj1)
        assertThat(calls).isEqualTo(1)
        assertThat(cache.get("id1", ::getObj1)).isEqualTo(obj1)
        assertThat(calls).isEqualTo(1)
        assertThat(cache.get("id1", ::getObj2)).isEqualTo(obj1)
        assertThat(calls).isEqualTo(1)
        assertThat(cache.allOfType<SimpleObject>()).containsExactly(obj1)

        assertThat(cache.get("id2", ::getObj2)).isEqualTo(obj2)
        assertThat(calls).isEqualTo(2)
        assertThat(cache.allOfType<SimpleObject>()).containsExactly(obj1, obj2)
    }

    @Test
    fun testTTLAlwaysValid() {
        val obj = SimpleObject(id = "id", name = "object")
        val alwaysValidCache = Cache(testCacheFile, Cache.TTLStrategy.AlwaysValid)

        alwaysValidCache.put("id", obj)
        alwaysValidCache.assertContains(obj)
    }

    @Test
    fun testTTLNeverValid() {
        val obj = SimpleObject(id = "id", name = "object")
        val neverValidCache = Cache(testCacheFile, Cache.TTLStrategy.NeverValid)

        neverValidCache.put("id", obj)
        assertThat(neverValidCache.cached).isEmpty()

        neverValidCache.put("id", obj)
        assertThat(neverValidCache.getCached("id")).isNull()
    }

    @Test
    fun testUniversalTTL() {
        val obj = SimpleObject(id = "id", name = "object")
        val ttlCache = Cache(testCacheFile, Cache.TTLStrategy.UniversalTTL(ttl = 5))

        ttlCache.put("id", obj)
        ttlCache.assertContains(obj)
        ttlCache.assertContains(obj)

        Thread.sleep(10)

        assertThat(ttlCache.cached).isEmpty()
        assertThat(ttlCache.getCached("id")).isNull()
    }

    @Test
    fun testTTLByClass() {
        val obj1 = SimpleObject(id = "id1", name = "object")
        val obj2 = SimpleObject2(id = "id2", name = "object")
        val ttlCache = Cache(
            testCacheFile,
            Cache.TTLStrategy.TTLByClass(
                mapOf(
                    SimpleObject::class to 5,
                    SimpleObject2::class to 15
                )
            )
        )

        ttlCache.put("id1", obj1)
        ttlCache.put("id2", obj2)
        ttlCache.assertContains(obj1, obj2)

        Thread.sleep(10)

        ttlCache.assertContains(obj2)

        Thread.sleep(10)

        assertThat(ttlCache.cached).isEmpty()
    }

    @Test
    fun testSaveAndLoad() {
        val obj1 = SimpleObject(id = "id1", name = "obj1")
        val obj2 = SimpleObject(id = "id2", name = "obj2")
        val obj3 = SimpleObject(id = "id3", name = "obj3")

        cache.put("id1", obj1)
        cache.put("id2", obj2)

        cache.save()

        cache.assertContains(obj1, obj2)
        assertThat(cache.allOfType<SimpleObject>()).containsExactly(obj1, obj2)

        cache.put("id3", obj3)

        cache.assertContains(obj1, obj2, obj3)
        assertThat(cache.allOfType<SimpleObject>()).containsExactly(obj1, obj2, obj3)

        cache.load()

        cache.assertContains(obj1, obj2)
        assertThat(cache.allOfType<SimpleObject>()).containsExactly(obj1, obj2)
    }

    private fun Cache.assertContains(vararg objects: SpotifyObject) {
        objects.forEach { obj ->
            val cached = getCached(obj.id!!)
            assertThat(cached).isNotNull()
            assertThat(cached?.id).isEqualTo(obj.id)
            assertThat(cached?.obj).isEqualTo(obj)
        }
    }

    companion object {
        private val testCacheFile = File("test_cache.json")

        @AfterAll
        @JvmStatic
        @Suppress("unused")
        fun cleanup() {
            testCacheFile.delete()
        }

        @BeforeAll
        @JvmStatic
        @Suppress("unused")
        fun setup() {
            testCacheFile.delete() // delete if exists in case it was leftover from previous tests
        }
    }
}
