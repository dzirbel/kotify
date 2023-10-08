package com.dzirbel.kotify.util.collections

import assertk.assertThat
import com.dzirbel.kotify.util.containsExactlyElementsOf
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ZipTest {
    data class Case(val a: List<Int>, val b: List<Int>, val expected: List<Pair<Int, Int>>)

    @ParameterizedTest
    @MethodSource("cases")
    fun zipEach(case: Case) {
        val result = mutableListOf<Pair<Int, Int>>()
        case.a.zipEach(case.b) { a, b ->
            result.add(Pair(a, b))
        }

        assertThat(result).containsExactlyElementsOf(case.expected)
    }

    @ParameterizedTest
    @MethodSource("cases")
    fun zipLazy(case: Case) {
        val result = case.a.zipLazy(case.b)

        assertThat(result.toList()).containsExactlyElementsOf(case.expected)
    }

    companion object {
        @JvmStatic
        fun cases(): List<Case> {
            return listOf(
                Case(emptyList(), emptyList(), emptyList()),
                Case(listOf(1, 2, 3, 4, 5), listOf(8, 7, 6), listOf(1 to 8, 2 to 7, 3 to 6)),
                Case(listOf(1, 2, 3), listOf(8, 7, 6, 5, 4), listOf(1 to 8, 2 to 7, 3 to 6)),
            )
        }
    }
}
