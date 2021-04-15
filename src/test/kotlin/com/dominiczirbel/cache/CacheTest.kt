package com.dominiczirbel.cache

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File

// TODO test batch get/put
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

    private val events: MutableList<CacheEvent> = mutableListOf()
    private var timeOverride: Long? = null

    private fun createCache(
        saveOnChange: Boolean = false,
        ttlStrategy: CacheTTLStrategy = CacheTTLStrategy.AlwaysValid,
        replacementStrategy: CacheReplacementStrategy = CacheReplacementStrategy.AlwaysReplace,
    ): Cache {
        return Cache(
            file = testCacheFile,
            saveOnChange = saveOnChange,
            ttlStrategy = ttlStrategy,
            replacementStrategy = replacementStrategy,
            getCurrentTime = { timeOverride ?: System.currentTimeMillis() },
            eventHandler = { events.addAll(it) }
        )
    }

    @AfterEach
    fun cleanup() {
        testCacheFile.delete()
    }

    @Test
    fun testEmpty() {
        val cache = createCache()

        cache.assertContainsExactly()
        assertEvents(CacheEvent.Dump(cache = cache))

        cache.assertDoesNotContain("id")
        assertEvents(CacheEvent.Miss(cache = cache, id = "id"))
    }

    @Test
    fun testPutAndGet() {
        val cache = createCache()
        val obj = SimpleObject(id = "id", name = "object")
        val cacheObject = CacheObject(id = "id", obj = obj, cacheTime = 123)

        timeOverride = 123
        assertThat(cache.put("id", obj)).isTrue()
        assertEvents(CacheEvent.Update(cache = cache, id = "id", previous = null, new = cacheObject))

        cache.assertContainsExactly(obj)
        events.clear()

        val cachedObject = requireNotNull(cache.getCached("id"))
        assertThat(cachedObject.obj).isSameInstanceAs(obj)
        assertThat(cachedObject.id).isEqualTo(obj.id)
        assertThat(cachedObject.cacheTime).isEqualTo(123)
        assertEvents(CacheEvent.Hit(cache = cache, id = "id", value = cacheObject))
    }

    @Test
    fun testInvalidate() {
        val cache = createCache()
        val obj = SimpleObject(id = "id", name = "object")
        val cacheObject = CacheObject(id = "id", obj = obj, cacheTime = 123)

        timeOverride = 123
        cache.put("id", obj)

        cache.assertContainsExactly(obj)

        events.clear()
        assertThat(cache.invalidate("id")?.obj).isEqualTo(obj)
        assertEvents(CacheEvent.Invalidate(cache = cache, id = "id", value = cacheObject))

        cache.assertDoesNotContain("id")
    }

    @Test
    fun testRemoteCalls() {
        val cache = createCache()
        var calls = 0
        val obj1 = SimpleObject(id = "id1", name = "obj1")
        val obj2 = SimpleObject(id = "id2", name = "obj2")
        fun getObj1() = obj1.also { calls++ }
        fun getObj2() = obj2.also { calls++ }

        assertThat(calls).isEqualTo(0)

        assertThat(cache.get("id1", remote = ::getObj1)).isEqualTo(obj1)
        assertThat(calls).isEqualTo(1)
        assertThat(cache.get("id1", remote = ::getObj1)).isEqualTo(obj1)
        assertThat(calls).isEqualTo(1)
        assertThat(cache.get("id1", remote = ::getObj2)).isEqualTo(obj1)
        assertThat(calls).isEqualTo(1)
        cache.assertContainsExactly(obj1)

        assertThat(cache.get("id2", remote = ::getObj2)).isEqualTo(obj2)
        assertThat(calls).isEqualTo(2)
        cache.assertContainsExactly(obj1, obj2)
    }

    @Test
    fun testTTLAlwaysValid() {
        val cache = createCache(ttlStrategy = CacheTTLStrategy.AlwaysValid)
        val obj = SimpleObject(id = "id", name = "object")

        timeOverride = 0

        cache.put("id", obj)
        cache.assertContainsExactly(obj)

        timeOverride = 100

        cache.assertContainsExactly(obj)
    }

    @Test
    fun testTTLNeverValid() {
        val cache = createCache(ttlStrategy = CacheTTLStrategy.NeverValid)
        val obj = SimpleObject(id = "id", name = "object")

        cache.put("id", obj)
        cache.assertContainsExactly()
        cache.assertDoesNotContain("id")

        cache.put("id", obj)
        cache.assertContainsExactly()
        cache.assertDoesNotContain("id")
    }

    @Test
    fun testUniversalTTL() {
        val cache = createCache(ttlStrategy = CacheTTLStrategy.UniversalTTL(ttl = 20))
        val obj = SimpleObject(id = "id", name = "object")

        timeOverride = 0

        cache.put("id", obj)
        cache.assertContainsExactly(obj)

        timeOverride = 10

        cache.assertContainsExactly(obj)

        timeOverride = 30

        cache.assertContainsExactly()
    }

    @Test
    fun testTTLByClass() {
        val cache = createCache(
            ttlStrategy = CacheTTLStrategy.TTLByClass(
                mapOf(
                    SimpleObject::class to 5,
                    SimpleRecursiveObject::class to 15
                )
            )
        )
        val obj1 = SimpleObject(id = "id1", name = "object")
        val obj2 = SimpleRecursiveObject(id = "id2", name = "object")

        timeOverride = 0

        cache.put("id1", obj1)
        cache.put("id2", obj2)
        cache.assertContainsExactly(obj1, obj2)

        timeOverride = 10

        cache.assertContainsExactly(obj2)

        timeOverride = 20

        cache.assertContainsExactly()
    }

    @Test
    fun testAlwaysReplace() {
        val cache = createCache(replacementStrategy = CacheReplacementStrategy.AlwaysReplace)
        val obj1 = SimpleObject(id = "id", name = "obj1")
        val obj2 = SimpleObject(id = "id", name = "obj2")

        cache.put("id", obj1)
        cache.assertContainsExactly(obj1)

        cache.put("id", obj2)
        cache.assertContainsExactly(obj2)

        cache.put("id", obj1)
        cache.assertContainsExactly(obj1)
    }

    @Test
    fun testNeverReplace() {
        val cache = createCache(replacementStrategy = CacheReplacementStrategy.NeverReplace)
        val obj1 = SimpleObject(id = "id", name = "obj1")
        val obj2 = SimpleObject(id = "id", name = "obj2")

        cache.put("id", obj1)
        cache.assertContainsExactly(obj1)

        cache.put("id", obj2)
        cache.assertContainsExactly(obj1)

        cache.put("id", obj1)
        cache.assertContainsExactly(obj1)
    }

    @Test
    fun testPutRecursive() {
        val cache = createCache()
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
        val cache = createCache(saveOnChange = false)
        val obj1 = SimpleObject(id = "id1", name = "obj1")
        val obj2 = SimpleObject(id = "id2", name = "obj2")
        val obj3 = SimpleObject(id = "id3", name = "obj3")

        cache.put("id1", obj1)
        cache.put("id2", obj2)

        cache.save()

        cache.assertContainsExactly(obj1, obj2)

        cache.put("id3", obj3)

        cache.assertContainsExactly(obj1, obj2, obj3)

        cache.load()

        cache.assertContainsExactly(obj1, obj2)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testSaveOnChange(saveOnChange: Boolean) {
        val cache = createCache(saveOnChange = saveOnChange)
        val obj = SimpleObject(id = "id", name = "obj")

        cache.put(obj)
        cache.assertContainsExactly(obj)

        // wait for async write to finish
        Thread.sleep(100)

        cache.load()
        if (saveOnChange) cache.assertContainsExactly(obj) else cache.assertContainsExactly()
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testSaveOnChangeFromRemote(saveOnChange: Boolean) {
        val cache = createCache(saveOnChange = saveOnChange)
        val obj = SimpleObject(id = "id", name = "obj")

        cache.get("id2") { obj }
        cache.assertContainsExactly(obj)

        // wait for async write to finish
        Thread.sleep(100)

        cache.load()
        if (saveOnChange) cache.assertContainsExactly(obj) else cache.assertContainsExactly()
    }

    @RepeatedTest(10)
    fun testConcurrent() {
        val cache = createCache()
        runBlocking {
            var calls = 0
            val firstDeferred = async {
                cache.get("id1") {
                    delay(30)
                    calls++
                    SimpleObject(id = "id1", name = "obj")
                }
            }

            val secondDeferred = async {
                cache.get("id1") {
                    delay(20)
                    calls++
                    SimpleObject(id = "id1", name = "obj")
                }
            }

            val first = firstDeferred.await()
            val second = secondDeferred.await()

            assertThat(first).isEqualTo(second)
            assertThat(first).isNotSameInstanceAs(second)
            assertThat(calls).isEqualTo(2)
        }
    }

    private fun Cache.assertContains(vararg objects: CacheableObject) {
        objects.forEach { obj ->
            val cached = getCached(obj.id!!)
            assertWithMessage("didn't contain $obj").that(cached).isNotNull()
            assertThat(cached?.id).isEqualTo(obj.id)
            assertThat(cached?.obj).isEqualTo(obj)
        }
    }

    private fun Cache.assertDoesNotContain(vararg ids: String) {
        ids.forEach { id ->
            assertThat(getCached(id)).isNull()
        }
    }

    private fun Cache.assertContainsExactly(vararg objects: CacheableObject) {
        assertContains(*objects)
        assertThat(getCache().values.map { it.obj }).containsExactly(*objects)
        assertThat(size).isEqualTo(objects.size)
    }

    private fun assertEvents(vararg expected: CacheEvent, clear: Boolean = true) {
        assertThat(events).containsExactly(*expected).inOrder()
        if (clear) {
            events.clear()
        }
    }

    companion object {
        private val testCacheFile = File("test_cache.json")
    }
}
