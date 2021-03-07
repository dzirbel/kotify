package com.dominiczirbel.cache

import com.google.common.collect.Range
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File

internal class CacheTest {
    @Serializable
    private data class SimpleObject(
        override val id: String,
        val name: String,
    ) : CacheableObject

    @Serializable
    private data class SimpleRecursiveObject(
        override val id: String,
        val name: String,
        val objects: List<CacheableObject> = listOf()
    ) : CacheableObject {
        override val cacheableObjects: Collection<CacheableObject>
            get() = objects
    }

    private val cache = Cache(testCacheFile)

    @Test
    fun testEmpty() {
        assertThat(cache.getCached("id")).isNull()
        assertThat(cache.cache).isEmpty()
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

        cache.assertContainsExactly(obj)

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
        val alwaysValidCache = Cache(testCacheFile, ttlStrategy = Cache.TTLStrategy.AlwaysValid)

        alwaysValidCache.put("id", obj)
        alwaysValidCache.assertContainsExactly(obj)
    }

    @Test
    fun testTTLNeverValid() {
        val obj = SimpleObject(id = "id", name = "object")
        val neverValidCache = Cache(testCacheFile, ttlStrategy = Cache.TTLStrategy.NeverValid)

        neverValidCache.put("id", obj)
        assertThat(neverValidCache.cache).isEmpty()

        neverValidCache.put("id", obj)
        assertThat(neverValidCache.getCached("id")).isNull()
    }

    @Test
    fun testUniversalTTL() {
        val obj = SimpleObject(id = "id", name = "object")
        val ttlCache = Cache(testCacheFile, ttlStrategy = Cache.TTLStrategy.UniversalTTL(ttl = 5))

        ttlCache.put("id", obj)
        ttlCache.assertContainsExactly(obj)
        ttlCache.assertContainsExactly(obj)

        Thread.sleep(10)

        assertThat(ttlCache.cache).isEmpty()
        assertThat(ttlCache.getCached("id")).isNull()
    }

    @Test
    fun testTTLByClass() {
        val obj1 = SimpleObject(id = "id1", name = "object")
        val obj2 = SimpleRecursiveObject(id = "id2", name = "object")
        val ttlCache = Cache(
            testCacheFile,
            ttlStrategy = Cache.TTLStrategy.TTLByClass(
                mapOf(
                    SimpleObject::class to 5,
                    SimpleRecursiveObject::class to 15
                )
            )
        )

        ttlCache.put("id1", obj1)
        ttlCache.put("id2", obj2)
        ttlCache.assertContainsExactly(obj1, obj2)

        Thread.sleep(10)

        ttlCache.assertContainsExactly(obj2)

        Thread.sleep(10)

        assertThat(ttlCache.cache).isEmpty()
    }

    @Test
    fun testAlwaysReplace() {
        val obj1 = SimpleObject(id = "id", name = "obj1")
        val obj2 = SimpleObject(id = "id", name = "obj2")

        val cache = Cache(testCacheFile, replacementStrategy = Cache.ReplacementStrategy.AlwaysReplace)

        cache.put("id", obj1)
        cache.assertContainsExactly(obj1)

        cache.put("id", obj2)
        cache.assertContainsExactly(obj2)

        cache.put("id", obj1)
        cache.assertContainsExactly(obj1)
    }

    @Test
    fun testNeverReplace() {
        val obj1 = SimpleObject(id = "id", name = "obj1")
        val obj2 = SimpleObject(id = "id", name = "obj2")

        val cache = Cache(testCacheFile, replacementStrategy = Cache.ReplacementStrategy.NeverReplace)

        cache.put("id", obj1)
        cache.assertContainsExactly(obj1)

        cache.put("id", obj2)
        cache.assertContainsExactly(obj1)

        cache.put("id", obj1)
        cache.assertContainsExactly(obj1)
    }

    @Test
    fun testPutRecursive() {
        val obj1 = SimpleObject(id = "id1", name = "obj1")
        val obj2 = SimpleObject(id = "id2", name = "obj2")
        val obj3 = SimpleObject(id = "id3", name = "obj3")
        val obj4 = SimpleRecursiveObject(id = "id4", name = "obj4", objects = listOf(obj1, obj2, obj3))
        val obj5 = SimpleObject(id = "id5", name = "obj5")
        val obj6 = SimpleRecursiveObject(id = "id6", name = "obj6")
        val obj7 = SimpleRecursiveObject(id = "id7", name = "obj7", objects = listOf(obj4, obj5, obj6))

        cache.put(obj7)
        cache.assertContainsExactly(obj1, obj2, obj3, obj4, obj5, obj6, obj7)
    }

    @Test
    fun testSaveAndLoad() {
        val obj1 = SimpleObject(id = "id1", name = "obj1")
        val obj2 = SimpleObject(id = "id2", name = "obj2")
        val obj3 = SimpleObject(id = "id3", name = "obj3")

        cache.put("id1", obj1)
        cache.put("id2", obj2)

        cache.save()

        cache.assertContainsExactly(obj1, obj2)
        assertThat(cache.allOfType<SimpleObject>()).containsExactly(obj1, obj2)

        cache.put("id3", obj3)

        cache.assertContainsExactly(obj1, obj2, obj3)
        assertThat(cache.allOfType<SimpleObject>()).containsExactly(obj1, obj2, obj3)

        cache.load()

        cache.assertContainsExactly(obj1, obj2)
        assertThat(cache.allOfType<SimpleObject>()).containsExactly(obj1, obj2)
    }

    private fun Cache.assertContains(vararg objects: CacheableObject) {
        objects.forEach { obj ->
            val cached = getCached(obj.id!!)
            assertWithMessage("didn't contain $obj").that(cached).isNotNull()
            assertThat(cached?.id).isEqualTo(obj.id)
            assertThat(cached?.obj).isEqualTo(obj)
        }
    }

    private fun Cache.assertContainsExactly(vararg objects: CacheableObject) {
        assertContains(*objects)
        assertThat(cache.values.map { it.obj }).containsExactly(*objects)
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
