package com.dzirbel.kotify.util.collections

import assertk.assertThat
import com.dzirbel.kotify.util.isSorted
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class SortedIndexTest {
    @ParameterizedTest
    @MethodSource
    fun sortedIndexFor(list: List<Int>) {
        val comparator = Comparator.naturalOrder<Int>()
        assertThat(list).isSorted(comparator) // check that input list is sorted

        // try all values from the first - 1 to the last + 1
        val values = if (list.isEmpty()) listOf(0) else list.first() - 1..list.last() + 1

        for (value in values) {
            val index = list.sortedIndexFor(value, comparator)
            val inserted = list.toMutableList().apply { add(index = index, element = value) }
            assertThat(inserted).isSorted(comparator)
        }
    }

    companion object {
        @JvmStatic
        fun sortedIndexFor(): List<List<Int>> {
            return listOf(
                emptyList(),
                listOf(1),
                listOf(1, 1),
                listOf(1, 3),
                listOf(1, 3, 5),
                listOf(1, 2, 3),
                listOf(0, 0, 0),
                listOf(1, 2, 3, 3, 3, 5, 6, 10),
            )
        }
    }
}
