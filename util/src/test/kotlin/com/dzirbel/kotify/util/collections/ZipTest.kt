package com.dzirbel.kotify.util.collections

import assertk.assertThat
import assertk.assertions.containsExactly
import org.junit.jupiter.api.Test

class ZipTest {
    @Test
    fun zipEach() {
        val result = mutableListOf<Pair<Int, Int>>()
        listOf(1, 2, 3, 4, 5).zipEach(listOf(8, 7, 6)) { a, b ->
            result.add(Pair(a, b))
        }

        assertThat(result).containsExactly(Pair(1, 8), Pair(2, 7), Pair(3, 6))
    }

    @Test
    fun zipLazy() {
        val result = listOf(1, 2, 3, 4, 5).zipLazy(listOf(8, 7, 6))

        assertThat(result.toList()).containsExactly(Pair(1, 8), Pair(2, 7), Pair(3, 6))
    }
}
