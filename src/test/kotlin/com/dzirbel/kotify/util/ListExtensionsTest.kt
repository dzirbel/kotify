package com.dzirbel.kotify.util

import com.google.common.collect.Range
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

internal class ListExtensionsTest {
    data class PlusSortedCase(val list: List<String>, val elements: List<String>)

    @ParameterizedTest
    @MethodSource
    fun plusSorted(case: PlusSortedCase) {
        assertThat(case.list.isSortedBy { it.length }).isTrue()

        val result = case.list.plusSorted(elements = case.elements) { it.length }
        assertThat(result.isSortedBy { it.length }).isTrue()
    }

    /**
     * Simple test for [isSortedBy] since its logic is non-trivial.
     */
    @Test
    fun testIsSortedBy() {
        assertThat(listOf<String>().isSortedBy { it.length }).isTrue()

        assertThat(listOf("").isSortedBy { it.length }).isTrue()

        assertThat(listOf("a", "b").isSortedBy { it.length }).isTrue()

        assertThat(listOf("a", "bb").isSortedBy { it.length }).isTrue()

        assertThat(listOf("aa", "b").isSortedBy { it.length }).isFalse()

        assertThat(listOf("a", "bb", "cccc").isSortedBy { it.length }).isTrue()
    }

    @Test
    fun testMinusAt() {
        assertThat(listOf(1, 2, 3, 4).minusAt(0))
            .containsExactly(2, 3, 4)
            .inOrder()

        assertThat(listOf(1, 2, 3, 4).minusAt(3))
            .containsExactly(1, 2, 3)
            .inOrder()

        assertThat(listOf(1, 2, 3, 4).minusAt(2))
            .containsExactly(1, 2, 4)
            .inOrder()
    }

    @RepeatedTest(3)
    fun flatMapParallel() {
        val baseList = listOf(1, 3, 4, 10)
        fun transform(n: Int) = List(n) { x -> n * (x + 1) }

        var maxDelayMs = 0
        val result = runBlocking {
            val start = TimeSource.Monotonic.markNow()
            val result = baseList
                .flatMapParallel { n ->
                    val delay = 100 - n * 10
                    maxDelayMs = max(maxDelayMs, delay)

                    delay(delay.toLong())

                    transform(n)
                }

            val duration = start.elapsedNow()
            assertThat(duration).isIn(Range.closed(maxDelayMs.milliseconds, (maxDelayMs * 2).milliseconds))

            result
        }

        assertThat(result).isEqualTo(baseList.flatMap { transform(it) })
    }

    /**
     * Convenience function which determines whether this [List] is sorted in ascending order according to [selector].
     */
    private fun <T, R : Comparable<R>> List<T>.isSortedBy(selector: (T) -> R): Boolean {
        if (isEmpty()) return true

        var current = selector(this[0])
        for (element in this.drop(1)) {
            val selected = selector(element)
            if (selected < current) return false
            current = selected
        }
        return true
    }

    companion object {
        @JvmStatic
        fun plusSorted(): List<PlusSortedCase> {
            return listOf(
                PlusSortedCase(
                    list = listOf(),
                    elements = listOf(),
                ),
                PlusSortedCase(
                    list = listOf(),
                    elements = listOf("a"),
                ),
                PlusSortedCase(
                    list = listOf("a"),
                    elements = listOf(),
                ),
                PlusSortedCase(
                    list = listOf(),
                    elements = listOf("aaa", "bb", "c"),
                ),
                PlusSortedCase(
                    list = listOf("a", "b", "c", "aa", "bb", "cccc"),
                    elements = listOf("d", "ffffff", "eee", ""),
                ),
            )
        }
    }
}
