package com.dzirbel.kotify.ui.components.grid

import com.dzirbel.kotify.ui.components.sort.Sort
import com.dzirbel.kotify.ui.components.sort.SortOrder
import com.dzirbel.kotify.ui.components.sort.SortableProperty
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class ListAdapterTest {
    private val list = List(20) { it }

    private val naturalOrder = object : SortableProperty<Int>(sortTitle = "natural order") {
        override fun compare(first: IndexedValue<Int>, second: IndexedValue<Int>): Int {
            return first.value.compareTo(second.value)
        }
    }

    private val orderByMod2 = object : SortableProperty<Int>(sortTitle = "mod 2 order") {
        override fun compare(first: IndexedValue<Int>, second: IndexedValue<Int>): Int {
            return (first.value % 2).compareTo(second.value % 2)
        }
    }

    private class Mod3Divider(override val sortOrder: SortOrder) : SimpleGridDivider<Int>(dividerTitle = "mod 3") {
        override fun divisionFor(element: Int) = (element % 3).toString()
        override fun withSortOrder(sortOrder: SortOrder) = error("unimplemented")
    }

    @Test
    fun testPlainList() {
        val elements = ListAdapter(list)

        assertThat(elements.divisions)
            .isEqualTo(mapOf(null to list))
    }

    @Test
    fun testFilter() {
        val predicate: (Int) -> Boolean = { it % 2 == 0 }

        val elements = ListAdapter(list)
            .withFilter(predicate)

        assertThat(elements.divisions)
            .isEqualTo(mapOf(null to list.filter(predicate)))
    }

    @Test
    fun testSort() {
        val elementsDescending = ListAdapter(list)
            .withSort(listOf(Sort(naturalOrder, SortOrder.DESCENDING)))

        assertThat(elementsDescending.divisions)
            .isEqualTo(mapOf(null to list.reversed()))

        val elementsByMod2 = elementsDescending
            .withSort(listOf(Sort(orderByMod2, SortOrder.ASCENDING)))

        // sort should be stable: preserve descending sort after sorting by mod 2
        assertThat(elementsByMod2.divisions)
            .isEqualTo(mapOf(null to listOf(18, 16, 14, 12, 10, 8, 6, 4, 2, 0, 19, 17, 15, 13, 11, 9, 7, 5, 3, 1)))
    }

    @Test
    fun testDivided() {
        val elementsDescending = ListAdapter(list)
            .withDivisions(
                divisionFor = { (it % 3).toString() },
                divider = Mod3Divider(sortOrder = SortOrder.DESCENDING),
            )

        assertThat(elementsDescending.divisions)
            .containsExactly(
                "2", list.filter { it % 3 == 2 },
                "1", list.filter { it % 3 == 1 },
                "0", list.filter { it % 3 == 0 },
            )
            .inOrder()

        val elementsAscending = elementsDescending
            .withDivisions(
                divisionFor = { (it % 3).toString() },
                divider = Mod3Divider(sortOrder = SortOrder.ASCENDING),
            )

        assertThat(elementsAscending.divisions)
            .containsExactly(
                "0", list.filter { it % 3 == 0 },
                "1", list.filter { it % 3 == 1 },
                "2", list.filter { it % 3 == 2 },
            )
            .inOrder()
    }

    @Test
    fun testCombined() {
        val predicate: (Int) -> Boolean = { it % 2 == 0 }

        val elements = ListAdapter(list)
            .withFilter(predicate)
            .withSort(listOf(Sort(naturalOrder, SortOrder.DESCENDING)))
            .withDivisions(
                divisionFor = { (it % 3).toString() },
                divider = Mod3Divider(sortOrder = SortOrder.DESCENDING),
            )

        assertThat(elements.divisions)
            .containsExactly(
                "2", listOf(14, 8, 2),
                "1", listOf(16, 10, 4),
                "0", listOf(18, 12, 6, 0),
            )
            .inOrder()

        val elementsPlain = elements
            .withFilter { true }
            .withSort(null)
            .withDivisions(null, null)

        assertThat(elementsPlain.divisions)
            .isEqualTo(mapOf(null to list))
    }
}
