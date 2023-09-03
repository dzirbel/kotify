package com.dzirbel.kotify.util.coroutines

import assertk.all
import assertk.assertThat
import assertk.assertions.each
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class MergedMutexTest {
    @Test
    fun `lock and unlock`() {
        val mutexes = List(10) { Mutex() }
        val merged = MergedMutex(mutexes)

        runTest {
            assertThat(merged.isLocked).isFalse()
            assertThat(mutexes).each { it.transform { mutex -> mutex.isLocked }.isFalse() }

            merged.lock()

            assertThat(merged.isLocked).isTrue()
            assertThat(mutexes).each { it.transform { mutex -> mutex.isLocked }.isTrue() }

            merged.unlock()

            assertThat(merged.isLocked).isFalse()
            assertThat(mutexes).each { it.transform { mutex -> mutex.isLocked }.isFalse() }
        }
    }

    @Test
    fun `lock with owner`() {
        val mutexes = List(10) { Mutex() }
        val merged = MergedMutex(mutexes)
        val owner = Any()
        val nonOwner = Any()

        runTest {
            assertThat(merged.isLocked).isFalse()
            assertThat(merged.holdsLock(owner)).isFalse()
            assertThat(merged.holdsLock(nonOwner)).isFalse()
            assertThat(mutexes).each { assert ->
                assert.all {
                    transform { mutex -> mutex.isLocked }.isFalse()
                    transform { mutex -> mutex.holdsLock(owner) }.isFalse()
                    transform { mutex -> mutex.holdsLock(nonOwner) }.isFalse()
                }
            }

            merged.lock(owner = owner)

            assertThat(merged.isLocked).isTrue()
            assertThat(merged.holdsLock(owner)).isTrue()
            assertThat(merged.holdsLock(nonOwner)).isFalse()
            assertThat(mutexes).each { assert ->
                assert.all {
                    transform { mutex -> mutex.isLocked }.isTrue()
                    transform { mutex -> mutex.holdsLock(owner) }.isTrue()
                    transform { mutex -> mutex.holdsLock(nonOwner) }.isFalse()
                }
            }

            merged.unlock()

            assertThat(merged.isLocked).isFalse()
            assertThat(merged.holdsLock(owner)).isFalse()
            assertThat(merged.holdsLock(nonOwner)).isFalse()
            assertThat(mutexes).each { assert ->
                assert.all {
                    transform { mutex -> mutex.isLocked }.isFalse()
                    transform { mutex -> mutex.holdsLock(owner) }.isFalse()
                    transform { mutex -> mutex.holdsLock(nonOwner) }.isFalse()
                }
            }
        }
    }

    @Test
    fun `lock with one taken`() {
        val mutexes = List(10) { Mutex() }
        val specialMutex = mutexes[3]
        val merged = MergedMutex(mutexes)

        runTest {
            specialMutex.lock()

            assertThat(merged.isLocked).isFalse()

            val job = launch {
                merged.lock()
            }

            runCurrent()

            // merged lock is taken, but lock() has not yet returned
            assertThat(merged.isLocked).isTrue()
            assertThat(job.isActive).isTrue()

            specialMutex.unlock()

            runCurrent()

            // merged lock is still taken and lock() has now returned
            assertThat(merged.isLocked).isTrue()
            assertThat(job.isActive).isFalse()
        }
    }

    @Test
    fun `tryLock success`() {
        val mutexes = List(10) { Mutex() }
        val merged = MergedMutex(mutexes)

        runTest {
            assertThat(merged.isLocked).isFalse()
            assertThat(mutexes).each { it.transform { mutex -> mutex.isLocked }.isFalse() }

            assertThat(merged.tryLock()).isTrue()

            assertThat(merged.isLocked).isTrue()
            assertThat(mutexes).each { it.transform { mutex -> mutex.isLocked }.isTrue() }

            assertThat(merged.tryLock()).isFalse()

            assertThat(merged.isLocked).isTrue()
            assertThat(mutexes).each { it.transform { mutex -> mutex.isLocked }.isTrue() }

            merged.unlock()

            assertThat(merged.isLocked).isFalse()
            assertThat(mutexes).each { it.transform { mutex -> mutex.isLocked }.isFalse() }
        }
    }

    @Test
    fun `tryLock failure`() {
        val mutexes = List(10) { Mutex() }
        val specialMutex = mutexes[3]
        val merged = MergedMutex(mutexes)

        runTest {
            specialMutex.lock()

            assertThat(merged.isLocked).isFalse()
            for (mutex in mutexes) {
                assertThat(mutex.isLocked).isEqualTo(mutex == specialMutex)
            }

            assertThat(merged.tryLock()).isFalse()

            assertThat(merged.isLocked).isFalse()
            for (mutex in mutexes) {
                assertThat(mutex.isLocked).isEqualTo(mutex == specialMutex)
            }
        }
    }
}
