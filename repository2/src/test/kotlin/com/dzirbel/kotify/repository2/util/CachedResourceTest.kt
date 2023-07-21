package com.dzirbel.kotify.repository2.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class CachedResourceTest {
    private class TestCachedResource(
        scope: CoroutineScope,
        private val cachedValue: String? = "cached",
        private val remoteValue: String = "remote",
    ) {
        private var cacheGets = 0
        private var remoteGets = 0

        val resource = CachedResource(
            scope = scope,
            getFromCache = { cachedValue.also { cacheGets++ } },
            getFromRemote = { remoteValue.also { remoteGets++ } },
        )

        fun assertNoValue(): TestCachedResource {
            assertThat(resource.flow.value).isNull()
            return this
        }

        fun assertHasCachedValue(): TestCachedResource {
            assertThat(resource.flow.value).isEqualTo(cachedValue)
            return this
        }

        fun assertHasRemoteValue(): TestCachedResource {
            assertThat(resource.flow.value).isEqualTo(remoteValue)
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

            testCachedResource.resource.initFromCache()

            testCachedResource.assertNoValue().assertGets(cached = 0, remote = 0)

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

            testCachedResource.assertNoValue().assertGets(cached = 0, remote = 0)

            runCurrent()

            testCachedResource.assertHasCachedValue().assertGets(cached = 1, remote = 0)
        }
    }

    @Test
    fun `ensure loaded with cached value uses it`() {
        runTest {
            val testCachedResource = TestCachedResource(scope = this)

            testCachedResource.resource.ensureLoaded()

            testCachedResource.assertNoValue().assertGets(cached = 0, remote = 0)

            runCurrent()

            testCachedResource.assertHasCachedValue().assertGets(cached = 1, remote = 0)
        }
    }

    @Test
    fun `ensure loaded with no cached value uses remote`() {
        runTest {
            val testCachedResource = TestCachedResource(scope = this, cachedValue = null)

            testCachedResource.resource.ensureLoaded()

            testCachedResource.assertNoValue().assertGets(cached = 0, remote = 0)

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
            val testCachedResource = TestCachedResource(scope = this, cachedValue = null)

            testCachedResource.resource.initFromCache()
            runCurrent()

            testCachedResource.assertNoValue().assertGets(cached = 1, remote = 0)

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
}
