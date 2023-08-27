package com.dzirbel.kotify.util.immutable

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import com.dzirbel.kotify.util.isSorted
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class AddSortedTest {
    @ParameterizedTest
    @MethodSource
    fun addSorted(list: PersistentList<Int>) {
        val comparator = Comparator.naturalOrder<Int>()
        assertThat(list).isSorted(comparator) // check that input list is sorted

        // try all values from the first - 1 to the last + 1
        val values = if (list.isEmpty()) listOf(0) else list.first() - 1..list.last() + 1

        for (value in values) {
            val inserted = list.addSorted(value, comparator)
            assertThat(inserted).hasSize(list.size + 1)
            assertThat(inserted).contains(value)
            assertThat(inserted).isSorted(comparator)
        }
    }

    companion object {
        @JvmStatic
        fun addSorted(): List<PersistentList<Int>> {
            return listOf(
                persistentListOf(),
                persistentListOf(1),
                persistentListOf(1, 1),
                persistentListOf(1, 3),
                persistentListOf(1, 3, 5),
                persistentListOf(1, 2, 3),
                persistentListOf(0, 0, 0),
                persistentListOf(1, 2, 3, 3, 3, 5, 6, 10),
            )
        }
    }
}
