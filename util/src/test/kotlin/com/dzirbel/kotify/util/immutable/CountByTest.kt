package com.dzirbel.kotify.util.immutable

import assertk.assertThat
import assertk.assertions.containsOnly
import org.junit.jupiter.api.Test

class CountByTest {
    @Test
    fun countBy() {
        val list = listOf("aaa", "aba", "ab", "b", "a", "c", "b", "a", "da")

        val counts = list.countBy { it.last() }
        assertThat(counts).containsOnly('a' to 5, 'b' to 3, 'c' to 1)
    }
}
