package com.dzirbel.kotify.ui.components.adapter

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.dzirbel.kotify.containsExactly
import org.junit.jupiter.api.Test

internal class ListAdapterTest {
    private val list = List(20) { it }

    private val naturalOrder = object : SortableProperty<Int> {
        override val title = "natural order"

        override fun compare(sortOrder: SortOrder, first: Int, second: Int): Int {
            return sortOrder.compare(first, second)
        }
    }

    private val orderByMod2 = object : SortableProperty<Int> {
        override val title = "mod 2 order"

        override fun compare(sortOrder: SortOrder, first: Int, second: Int): Int {
            return sortOrder.compare(first % 2, second % 2)
        }
    }

    private object Mod3DividableProperty : DividableProperty<Int> {
        override val title = "mod 3"

        override fun divisionFor(element: Int): Int = element % 3
        override fun compareDivisions(sortOrder: SortOrder, first: Any?, second: Any?): Int {
            return sortOrder.compare(first as Int, second as Int)
        }
    }

    @Test
    fun testPlainList() {
        val elements = ListAdapter.from(list)

        assertThat(elements.divisions).isEqualTo(mapOf(null to list.withIndex().toList()))
        assertThat(elements.size).isEqualTo(list.size)

        list.forEachIndexed { index, element ->
            assertThat(elements[index]).isEqualTo(element)
        }
    }

    @Test
    fun testIterator() {
        val elements = ListAdapter.from(list)

        var element = 0
        elements.iterator().forEach {
            assertThat(it).isEqualTo(element)
            element++
        }
    }

    @Test
    fun testFilter() {
        val predicate: (Int) -> Boolean = { it % 2 == 0 }

        val elements = ListAdapter.from(list)
            .withFilter(filter = predicate)

        assertThat(elements.divisions)
            .isEqualTo(mapOf(null to list.withIndex().filter { predicate(it.value) }))
    }

    @Test
    fun testFilterByString() {
        val elements = ListAdapter.from(list)
            .withFilterByString(filterString = "0", elementProperty = { it.toString() })

        assertThat(elements.divisions)
            .isEqualTo(mapOf(null to list.withIndex().filter { it.toString().contains('0') }))
    }

    @Test
    fun testSort() {
        val elementsDescending = ListAdapter.from(list)
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
        val elementsDescending = ListAdapter.from(list)
            .withDivider(Divider(Mod3DividableProperty, SortOrder.DESCENDING))

        assertThat(elementsDescending.divisions)
            .containsExactly(
                2 to list.filter { it % 3 == 2 }.map { IndexedValue(it, it) },
                1 to list.filter { it % 3 == 1 }.map { IndexedValue(it, it) },
                0 to list.filter { it % 3 == 0 }.map { IndexedValue(it, it) },
            )

        val elementsAscending = elementsDescending
            .withDivider(Divider(Mod3DividableProperty, SortOrder.ASCENDING))

        assertThat(elementsAscending.divisions)
            .containsExactly(
                0 to list.filter { it % 3 == 0 }.map { IndexedValue(it, it) },
                1 to list.filter { it % 3 == 1 }.map { IndexedValue(it, it) },
                2 to list.filter { it % 3 == 2 }.map { IndexedValue(it, it) },
            )
    }

    @Test
    fun testCombined() {
        val predicate: (Int) -> Boolean = { it % 2 == 0 }

        val elements = ListAdapter.from(list)
            .withFilter(filter = predicate)
            .withSort(listOf(Sort(naturalOrder, SortOrder.DESCENDING)))
            .withDivider(Divider(Mod3DividableProperty, SortOrder.DESCENDING))

        assertThat(elements.divisions)
            .containsExactly(
                2 to listOf(14, 8, 2).map { IndexedValue(it, it) },
                1 to listOf(16, 10, 4).map { IndexedValue(it, it) },
                0 to listOf(18, 12, 6, 0).map { IndexedValue(it, it) },
            )

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

        val elements = ListAdapter.from(listOf(0, 1, 2, 3, 4, 5, 16, 17, 18, 19, 20))
            .withFilter(filter = predicate)
            .withSort(listOf(Sort(naturalOrder, SortOrder.DESCENDING)))
            .withDivider(Divider(Mod3DividableProperty, SortOrder.DESCENDING))
            .plusElements(listOf(6, 7, 50))

        assertThat(elements.divisions)
            .containsExactly(
                2 to listOf(IndexedValue(13, 50), IndexedValue(10, 20), IndexedValue(2, 2)),
                1 to listOf(IndexedValue(6, 16), IndexedValue(4, 4)),
                0 to listOf(IndexedValue(8, 18), IndexedValue(11, 6), IndexedValue(0, 0)),
            )
    }
}
