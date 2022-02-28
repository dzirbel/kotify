package com.dzirbel.kotify.ui.components.adapter

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

    private class Mod3Divider(divisionSortOrder: SortOrder) : Divider<Int>("mod 3", divisionSortOrder) {
        override fun divisionFor(element: Int) = (element % 3).toString()
        override fun withDivisionSortOrder(sortOrder: SortOrder) = error("unimplemented")
    }

    @Test
    fun testPlainList() {
        val elements = ListAdapter(list)

        assertThat(elements.divisions)
            .isEqualTo(mapOf(null to list.withIndex().toList()))
        assertThat(elements.size).isEqualTo(list.size)

        list.forEachIndexed { index, element ->
            assertThat(elements[index]).isEqualTo(element)
        }
    }

    @Test
    fun testIterator() {
        val elements = ListAdapter(list)

        var element = 0
        elements.iterator().forEach {
            assertThat(it).isEqualTo(element)
            element++
        }
    }

    @Test
    fun testFilter() {
        val predicate: (Int) -> Boolean = { it % 2 == 0 }

        val elements = ListAdapter(list)
            .withFilter(predicate)

        assertThat(elements.divisions)
            .isEqualTo(mapOf(null to list.withIndex().filter { predicate(it.value) }))
    }

    @Test
    fun testSort() {
        val elementsDescending = ListAdapter(list)
            .withSort(listOf(Sort(naturalOrder, SortOrder.DESCENDING)))

        assertThat(elementsDescending.divisions)
            .isEqualTo(mapOf(null to list.withIndex().reversed()))

        val elementsByMod2 = elementsDescending
            .withSort(listOf(Sort(orderByMod2, SortOrder.ASCENDING)))

        // sort should be stable: preserve descending sort after sorting by mod 2
        assertThat(elementsByMod2.divisions)
            .isEqualTo(
                mapOf(
                    null to listOf(18, 16, 14, 12, 10, 8, 6, 4, 2, 0, 19, 17, 15, 13, 11, 9, 7, 5, 3, 1)
                        .map { IndexedValue(it, it) }
                )
            )
    }

    @Test
    fun testDivided() {
        val elementsDescending = ListAdapter(list)
            .withDivider(Mod3Divider(divisionSortOrder = SortOrder.DESCENDING))

        assertThat(elementsDescending.divisions)
            .containsExactly(
                "2", list.filter { it % 3 == 2 }.map { IndexedValue(it, it) },
                "1", list.filter { it % 3 == 1 }.map { IndexedValue(it, it) },
                "0", list.filter { it % 3 == 0 }.map { IndexedValue(it, it) },
            )
            .inOrder()

        val elementsAscending = elementsDescending
            .withDivider(Mod3Divider(divisionSortOrder = SortOrder.ASCENDING))

        assertThat(elementsAscending.divisions)
            .containsExactly(
                "0", list.filter { it % 3 == 0 }.map { IndexedValue(it, it) },
                "1", list.filter { it % 3 == 1 }.map { IndexedValue(it, it) },
                "2", list.filter { it % 3 == 2 }.map { IndexedValue(it, it) },
            )
            .inOrder()
    }

    @Test
    fun testCombined() {
        val predicate: (Int) -> Boolean = { it % 2 == 0 }

        val elements = ListAdapter(list)
            .withFilter(predicate)
            .withSort(listOf(Sort(naturalOrder, SortOrder.DESCENDING)))
            .withDivider(Mod3Divider(divisionSortOrder = SortOrder.DESCENDING))

        assertThat(elements.divisions)
            .containsExactly(
                "2", listOf(14, 8, 2).map { IndexedValue(it, it) },
                "1", listOf(16, 10, 4).map { IndexedValue(it, it) },
                "0", listOf(18, 12, 6, 0).map { IndexedValue(it, it) },
            )
            .inOrder()

        val elementsPlain = elements
            .withFilter { true }
            .withSort(null)
            .withDivider(null)

        assertThat(elementsPlain.divisions)
            .isEqualTo(mapOf(null to list.withIndex().toList()))
    }

    @Test
    fun testPlusElements() {
        val predicate: (Int) -> Boolean = { it % 2 == 0 }

        val elements = ListAdapter(listOf(0, 1, 2, 3, 4, 5, 16, 17, 18, 19, 20))
            .withFilter(predicate)
            .withSort(listOf(Sort(naturalOrder, SortOrder.DESCENDING)))
            .withDivider(Mod3Divider(divisionSortOrder = SortOrder.DESCENDING))
            .plusElements(listOf(6, 7, 50))

        assertThat(elements.divisions)
            .containsExactly(
                "2", listOf(IndexedValue(13, 50), IndexedValue(10, 20), IndexedValue(2, 2)),
                "1", listOf(IndexedValue(6, 16), IndexedValue(4, 4)),
                "0", listOf(IndexedValue(8, 18), IndexedValue(11, 6), IndexedValue(0, 0)),
            )
            .inOrder()
    }
}
