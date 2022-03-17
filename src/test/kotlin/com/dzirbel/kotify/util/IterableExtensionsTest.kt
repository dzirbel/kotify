package com.dzirbel.kotify.util

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.dzirbel.kotify.containsExactly
import org.junit.jupiter.api.Test

class IterableExtensionsTest {
    @Test
    fun zipEach() {
        val result = mutableListOf<Pair<Int, Int>>()
        listOf(1, 2, 3, 4, 5).zipEach(listOf(8, 7, 6)) { a, b ->
            result.add(Pair(a, b))
        }

        assertThat(result).containsExactly(Pair(1, 8), Pair(2, 7), Pair(3, 6))
    }

    @Test
    fun zipToMap() {
        val map = listOf(1, 1, 2, 2, 3, 4, 5).zipToMap(listOf("a", "b", "c", "d", "e", "f"))

        assertThat(map).containsExactly(
            1 to "b",
            2 to "d",
            3 to "e",
            4 to "f",
        )
    }

    @Test
    fun averageOrNull() {
        val avg = listOf(1, null, 2, 3, 4, 5, null, 5).averageOrNull { it * 1.5 }
        assertThat(avg).isEqualTo(5.0)
    }
}
