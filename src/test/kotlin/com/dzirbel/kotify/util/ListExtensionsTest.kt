package com.dzirbel.kotify.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

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
