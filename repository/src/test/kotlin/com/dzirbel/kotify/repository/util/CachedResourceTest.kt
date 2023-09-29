package com.dzirbel.kotify.repository.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.matchesPredicate
import com.dzirbel.kotify.repository.CacheState
import com.dzirbel.kotify.util.CurrentTime
import com.dzirbel.kotify.util.MockedTimeExtension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockedTimeExtension::class)
class CachedResourceTest {
    private class TestCachedResource(
        scope: CoroutineScope,
        private val cachedValue: () -> String? = { "cached" },
        private val remoteValue: () -> String? = { "remote" },
    ) {
        private var cacheGets = 0
        private var remoteGets = 0

        val resource = CachedResource(
            scope = scope,
            getFromCache = {
                cacheGets++
                cachedValue()?.let { it to CurrentTime.instant }
            },
            getFromRemote = {
                remoteGets++
                remoteValue()?.let { it to CurrentTime.instant }
            },
        )

        fun assertNullValue(): TestCachedResource {
            assertThat(resource.flow.value).isNull()
            return this
        }

        fun assertRefreshing(): TestCachedResource {
            assertThat(resource.flow.value is CacheState.Refreshing).isTrue()
            return this
        }

        fun assertHasCachedValue(): TestCachedResource {
            assertThat(resource.flow.value?.cachedValue).isEqualTo(cachedValue())
            return this
        }

        fun assertHasRemoteValue(): TestCachedResource {
            assertThat(resource.flow.value?.cachedValue).isEqualTo(remoteValue())
            return this
        }

        fun assertError(): TestCachedResource {
            assertThat(resource.flow.value).matchesPredicate { it is CacheState.Error }
            return this
        }

        fun assertNotFound(): TestCachedResource {
            assertThat(resource.flow.value).matchesPredicate { it is CacheState.NotFound }
            return this
        }

        fun assertGets(cached: Int, remote: Int): TestCachedResource {
            assertThat(cacheGets).isEqualTo(cached)
            assertThat(remoteGets).isEqualTo(remote)
            return this
        }
    }

    @Test
    fun `multiple init calls are a no-op`() {
        runTest {
            val testCachedResource = TestCachedResource(scope = this)
            testCachedResource.assertNullValue().assertGets(cached = 0, remote = 0)

            testCachedResource.resource.initFromCache()

            testCachedResource.assertRefreshing().assertGets(cached = 0, remote = 0)

            runCurrent()

            testCachedResource.assertHasCachedValue().assertGets(cached = 1, remote = 0)

            testCachedResource.resource.initFromCache()

            testCachedResource.assertHasCachedValue().assertGets(cached = 1, remote = 0)

            runCurrent()

            testCachedResource.assertHasCachedValue().assertGets(cached = 1, remote = 0)
        }
    }

    @Test
    fun `concurrent init calls only trigger a single load from cache`() {
        runTest {
            val testCachedResource = TestCachedResource(scope = this)

            testCachedResource.resource.initFromCache()
            testCachedResource.resource.initFromCache()

            testCachedResource.assertRefreshing().assertGets(cached = 0, remote = 0)

            runCurrent()

            testCachedResource.assertHasCachedValue().assertGets(cached = 1, remote = 0)
        }
    }

    @Test
    fun `ensure loaded with cached value uses it`() {
        runTest {
            val testCachedResource = TestCachedResource(scope = this)

            testCachedResource.resource.ensureLoaded()

            testCachedResource.assertRefreshing().assertGets(cached = 0, remote = 0)

            runCurrent()

            testCachedResource.assertHasCachedValue().assertGets(cached = 1, remote = 0)
        }
    }

    @Test
    fun `ensure loaded with no cached value uses remote`() {
        runTest {
            val testCachedResource = TestCachedResource(scope = this, cachedValue = { null })

            testCachedResource.resource.ensureLoaded()

            testCachedResource.assertRefreshing().assertGets(cached = 0, remote = 0)

            runCurrent()

            testCachedResource.assertHasRemoteValue().assertGets(cached = 1, remote = 1)
        }
    }

    @Test
    fun `init with cached value then ensure loaded`() {
        runTest {
            val testCachedResource = TestCachedResource(scope = this)

            testCachedResource.resource.initFromCache()
            runCurrent()

            testCachedResource.assertHasCachedValue().assertGets(cached = 1, remote = 0)

            testCachedResource.resource.ensureLoaded()
            runCurrent()

            testCachedResource.assertHasCachedValue().assertGets(cached = 1, remote = 0)
        }
    }

    @Test
    fun `init with no cached value then ensure loaded`() {
        runTest {
            val testCachedResource = TestCachedResource(scope = this, cachedValue = { null })

            testCachedResource.resource.initFromCache()
            runCurrent()

            testCachedResource.assertNullValue().assertGets(cached = 1, remote = 0)

            testCachedResource.resource.ensureLoaded()
            runCurrent()

            testCachedResource.assertHasRemoteValue().assertGets(cached = 1, remote = 1)
        }
    }

    @Test
    fun `concurrent init with cached value and ensure loaded`() {
        runTest {
            val testCachedResource = TestCachedResource(scope = this)

            testCachedResource.resource.initFromCache()
            testCachedResource.resource.ensureLoaded()
            runCurrent()

            testCachedResource.assertHasCachedValue().assertGets(cached = 1, remote = 0)

            testCachedResource.resource.ensureLoaded()
            runCurrent()

            testCachedResource.assertHasCachedValue().assertGets(cached = 1, remote = 0)
        }
    }

    @Test
    fun `refresh from remote gets from remote again`() {
        runTest {
            val testCachedResource = TestCachedResource(scope = this)

            testCachedResource.resource.initFromCache()
            testCachedResource.resource.ensureLoaded()
            runCurrent()

            testCachedResource.assertHasCachedValue().assertGets(cached = 1, remote = 0)

            testCachedResource.resource.refreshFromRemote()
            runCurrent()

            testCachedResource.assertHasRemoteValue().assertGets(cached = 1, remote = 1)

            testCachedResource.resource.refreshFromRemote()
            runCurrent()

            testCachedResource.assertHasRemoteValue().assertGets(cached = 1, remote = 2)
        }
    }

    @Test
    fun `error loading from cache`() {
        runTest {
            val testCachedResource = TestCachedResource(scope = this, cachedValue = { error("error") })

            testCachedResource.resource.initFromCache()

            testCachedResource.assertRefreshing().assertGets(cached = 0, remote = 0)

            runCurrent()

            testCachedResource.assertError().assertGets(cached = 1, remote = 0)
        }
    }

    @Test
    fun `init with no cached value then ensure loaded and remote error`() {
        runTest {
            val testCachedResource = TestCachedResource(
                scope = this,
                cachedValue = { null },
                remoteValue = { error("error") },
            )

            testCachedResource.resource.initFromCache()
            runCurrent()

            testCachedResource.assertNullValue().assertGets(cached = 1, remote = 0)

            testCachedResource.resource.ensureLoaded()
            runCurrent()

            testCachedResource.assertError().assertGets(cached = 1, remote = 1)
        }
    }

    @Test
    fun `init with no cached value then ensure loaded and remote not found`() {
        runTest {
            val testCachedResource = TestCachedResource(
                scope = this,
                cachedValue = { null },
                remoteValue = { null },
            )

            testCachedResource.resource.initFromCache()
            runCurrent()

            testCachedResource.assertNullValue().assertGets(cached = 1, remote = 0)

            testCachedResource.resource.ensureLoaded()
            runCurrent()

            testCachedResource.assertNotFound().assertGets(cached = 1, remote = 1)
        }
    }
}
