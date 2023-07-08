package com.dzirbel.kotify.repository2.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class JobLockTest {
    @Test
    fun jobIsLaunched() {
        val lock = JobLock()
        var launched = false

        runTest {
            lock.launch(scope = this) {
                launched = true
            }

            assertThat(launched).isFalse()

            runCurrent()

            assertThat(launched).isTrue()
        }
    }

    @Test
    fun sequentialJobsAreLaunched() {
        val lock = JobLock()
        var launches = 0

        runTest {
            repeat(10) { repetition ->
                lock.launch(scope = this) {
                    launches++
                }

                assertThat(launches).isEqualTo(repetition)

                runCurrent()

                assertThat(launches).isEqualTo(repetition + 1)
            }
        }
    }

    @Test
    fun parallelJobsAreIgnored() {
        val lock = JobLock()
        var launches = 0
        var completions = 0

        runTest {
            repeat(5) {
                lock.launch(scope = this) {
                    launches++
                    delay(100)
                    completions++
                }
            }

            assertThat(launches).isEqualTo(0)
            assertThat(completions).isEqualTo(0)

            runCurrent()

            assertThat(launches).isEqualTo(1)
            assertThat(completions).isEqualTo(0)

            // another launch while job is still running is ignored
            lock.launch(scope = this) {
                launches++
                delay(100)
                completions++
            }

            advanceUntilIdle()

            assertThat(launches).isEqualTo(1)
            assertThat(completions).isEqualTo(1)
        }
    }

    @Test
    fun exceptionsArePropagated() {
        val lock = JobLock()
        var launches = 0
        var reachedEnd = false

        assertThrows<IllegalStateException> {
            runTest {
                lock.launch(scope = this) {
                    launches++
                    error("")
                }

                assertThat(launches).isEqualTo(0)

                runCurrent()

                assertThat(launches).isEqualTo(1)

                // second job is never launched due to exception in the first
                lock.launch(scope = this) {
                    launches++
                }

                runCurrent()

                assertThat(launches).isEqualTo(1)

                // exceptions thrown in job should not be thrown in this block
                reachedEnd = true
            }
        }

        assertThat(reachedEnd).isTrue()
        assertThat(launches).isEqualTo(1)
    }
}
