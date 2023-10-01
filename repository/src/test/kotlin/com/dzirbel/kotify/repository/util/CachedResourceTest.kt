package com.dzirbel.kotify.repository.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.matchesPredicate
import com.dzirbel.kotify.repository.CacheState
import com.dzirbel.kotify.repository.CacheStrategy
import com.dzirbel.kotify.util.CurrentTime
import com.dzirbel.kotify.util.MockedTimeExtension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

private class TestCachedResource(
    scope: CoroutineScope,
    private val cachedValue: () -> String? = { "cached" },
    private val remoteValue: () -> String? = { "remote" },
    private val cacheDelay: Long = 0,
    private val remoteDelay: Long = 0,
    cacheStrategy: CacheStrategy<Pair<String, Instant>> = CacheStrategy.AlwaysValid(),
) {
    private var cacheGets = 0
    private var remoteGets = 0

    val resource = CachedResource(
        scope = scope,
        getFromCache = {
            cacheGets++
            delay(cacheDelay)
            cachedValue()?.let { it to CurrentTime.instant }
        },
        getFromRemote = {
            remoteGets++
            delay(remoteDelay)
            remoteValue()?.let { it to CurrentTime.instant }
        },
        cacheStrategy = cacheStrategy,
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

@ExtendWith(MockedTimeExtension::class)
class CachedResourceTest {
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
    fun `sequential init calls only trigger a single load from cache`() {
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
    fun `concurrent init calls only trigger a single load from cache`() {
        runTest {
            val testCachedResource = TestCachedResource(scope = this)

            val jobs = withContext(Dispatchers.IO) {
                (1..10).map {
                    launch {
                        testCachedResource.resource.initFromCache()
                    }
                }
            }

            jobs.joinAll()

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

    @Test
    fun `init with invalid value then ensure loaded`() {
        runTest {
            val testCachedResource = TestCachedResource(
                scope = this,
                cacheStrategy = CacheStrategy.NeverValid(),
            )

            testCachedResource.resource.initFromCache()
            runCurrent()

            testCachedResource.assertNullValue().assertGets(cached = 1, remote = 0)

            testCachedResource.resource.ensureLoaded()
            runCurrent()

            testCachedResource.assertHasRemoteValue().assertGets(cached = 1, remote = 1)
        }
    }

    @Test
    fun `init with transient value then ensure loaded`() {
        runTest {
            val testCachedResource = TestCachedResource(
                scope = this,
                cacheStrategy = { CacheStrategy.CacheValidity.TRANSIENT },
            )

            testCachedResource.resource.initFromCache()
            runCurrent()

            testCachedResource.assertHasCachedValue().assertGets(cached = 1, remote = 0)

            testCachedResource.resource.ensureLoaded()
            runCurrent()

            testCachedResource.assertHasRemoteValue().assertGets(cached = 1, remote = 1)
        }
    }

    @Test
    fun `ensure loaded with invalid cached value`() {
        runTest {
            val testCachedResource = TestCachedResource(
                scope = this,
                cacheStrategy = CacheStrategy.NeverValid(),
            )

            testCachedResource.resource.ensureLoaded()
            runCurrent()

            testCachedResource.assertHasRemoteValue().assertGets(cached = 1, remote = 1)
        }
    }

    @Test
    fun `ensure loaded with transient cached value`() {
        runTest {
            val testCachedResource = TestCachedResource(
                scope = this,
                cacheStrategy = { CacheStrategy.CacheValidity.TRANSIENT },
            )

            testCachedResource.resource.ensureLoaded()
            runCurrent()

            testCachedResource.assertHasRemoteValue().assertGets(cached = 1, remote = 1)
        }
    }

    @Test
    fun `ensure loaded with invalid cached value, with delays`() {
        runTest {
            val testCachedResource = TestCachedResource(
                scope = this,
                cacheDelay = 100,
                remoteDelay = 100,
                cacheStrategy = CacheStrategy.NeverValid(),
            )

            testCachedResource.resource.ensureLoaded()
            runCurrent()

            testCachedResource.assertRefreshing().assertGets(cached = 1, remote = 0)

            advanceTimeBy(100)
            runCurrent()

            testCachedResource.assertRefreshing().assertGets(cached = 1, remote = 1)

            advanceTimeBy(100)
            runCurrent()

            testCachedResource.assertHasRemoteValue().assertGets(cached = 1, remote = 1)
        }
    }

    @Test
    fun `ensure loaded with transient cached value, with delays`() {
        runTest {
            val testCachedResource = TestCachedResource(
                scope = this,
                cacheDelay = 100,
                remoteDelay = 100,
                cacheStrategy = { CacheStrategy.CacheValidity.TRANSIENT },
            )

            testCachedResource.resource.ensureLoaded()
            runCurrent()

            testCachedResource.assertRefreshing().assertGets(cached = 1, remote = 0)

            advanceTimeBy(100)
            runCurrent()

            testCachedResource.assertHasCachedValue().assertGets(cached = 1, remote = 1)

            advanceTimeBy(100)
            runCurrent()

            testCachedResource.assertHasRemoteValue().assertGets(cached = 1, remote = 1)
        }
    }
}
