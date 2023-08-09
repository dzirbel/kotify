package com.dzirbel.kotify.util.coroutines

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.math.max

class MapParallelTest {
    @Test
    fun mapParallel() {
        val baseList = listOf(1, 3, 4, 10)
        fun transform(n: Int) = 3 * n + 1

        var maxDelayMs = 0L
        runTest {
            val start = testScheduler.currentTime

            val result = baseList
                .mapParallel { n ->
                    val delay = (100 - n * 10).toLong()
                    maxDelayMs = max(maxDelayMs, delay)

                    delay(delay)

                    transform(n)
                }

            val duration = testScheduler.currentTime - start
            assertThat(duration).isEqualTo(maxDelayMs)
            assertThat(result).isEqualTo(baseList.map(::transform))
        }
    }

    @Test
    fun flatMapParallel() {
        val baseList = listOf(1, 3, 4, 10)
        fun transform(n: Int) = List(n) { x -> n * (x + 1) }

        var maxDelayMs = 0L
        runTest {
            val start = testScheduler.currentTime

            val result = baseList
                .flatMapParallel { n ->
                    val delay = (100 - n * 10).toLong()
                    maxDelayMs = max(maxDelayMs, delay)

                    delay(delay)

                    transform(n)
                }

            val duration = testScheduler.currentTime - start
            assertThat(duration).isEqualTo(maxDelayMs)
            assertThat(result).isEqualTo(baseList.flatMap(::transform))
        }
    }
}
