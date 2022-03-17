package com.dzirbel.kotify.ui.components.adapter

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThan
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class SortOrderTest {
    data class CompareNullableCase(
        val sortOrder: SortOrder,
        val first: String?,
        val second: String?,
        val nullsFirst: Boolean,
    )

    data class SortNullableCase(val sortOrder: SortOrder, val nullsFirst: Boolean)

    @ParameterizedTest
    @MethodSource
    fun testCompareNullable(case: CompareNullableCase) {
        if (case.first == case.second) {
            assertThat(case.sortOrder.compareNullable(case.first, case.second, nullsFirst = case.nullsFirst))
                .isEqualTo(0)
            assertThat(case.sortOrder.compareNullable(case.second, case.first, nullsFirst = case.nullsFirst))
                .isEqualTo(0)
        } else {
            assertThat(case.sortOrder.compareNullable(case.first, case.second, nullsFirst = case.nullsFirst))
                .isLessThan(0)
            assertThat(case.sortOrder.compareNullable(case.second, case.first, nullsFirst = case.nullsFirst))
                .isGreaterThan(0)
        }
    }

    @ParameterizedTest
    @MethodSource
    fun testSortNullable(case: SortNullableCase) {
        val list = listOf(null, "b", null, "c", null, "a", null)
            .sortedWith { o1, o2 -> case.sortOrder.compareNullable(o1, o2, nullsFirst = case.nullsFirst) }

        val expected = if (case.nullsFirst) {
            when (case.sortOrder) {
                SortOrder.ASCENDING -> listOf(null, null, null, null, "a", "b", "c")
                SortOrder.DESCENDING -> listOf(null, null, null, null, "c", "b", "a")
            }
        } else {
            when (case.sortOrder) {
                SortOrder.ASCENDING -> listOf("a", "b", "c", null, null, null, null)
                SortOrder.DESCENDING -> listOf("c", "b", "a", null, null, null, null)
            }
        }

        assertThat(list).isEqualTo(expected)
    }

    companion object {
        @JvmStatic
        fun testCompareNullable(): List<CompareNullableCase> {
            return listOf(
                CompareNullableCase(first = "a", second = "a", nullsFirst = true, sortOrder = SortOrder.ASCENDING),
                CompareNullableCase(first = "a", second = "a", nullsFirst = false, sortOrder = SortOrder.ASCENDING),
                CompareNullableCase(first = "a", second = "a", nullsFirst = true, sortOrder = SortOrder.DESCENDING),
                CompareNullableCase(first = "a", second = "a", nullsFirst = false, sortOrder = SortOrder.DESCENDING),

                CompareNullableCase(first = "a", second = "b", nullsFirst = false, sortOrder = SortOrder.ASCENDING),
                CompareNullableCase(first = "a", second = "b", nullsFirst = true, sortOrder = SortOrder.ASCENDING),

                CompareNullableCase(first = "b", second = "a", nullsFirst = false, sortOrder = SortOrder.DESCENDING),
                CompareNullableCase(first = "b", second = "a", nullsFirst = true, sortOrder = SortOrder.DESCENDING),

                CompareNullableCase(first = "a", second = null, nullsFirst = false, sortOrder = SortOrder.ASCENDING),
                CompareNullableCase(first = "a", second = null, nullsFirst = false, sortOrder = SortOrder.DESCENDING),
                CompareNullableCase(first = null, second = "a", nullsFirst = true, sortOrder = SortOrder.ASCENDING),
                CompareNullableCase(first = null, second = "a", nullsFirst = true, sortOrder = SortOrder.DESCENDING),

                CompareNullableCase(first = null, second = null, nullsFirst = true, sortOrder = SortOrder.ASCENDING),
                CompareNullableCase(first = null, second = null, nullsFirst = true, sortOrder = SortOrder.DESCENDING),
                CompareNullableCase(first = null, second = null, nullsFirst = false, sortOrder = SortOrder.ASCENDING),
                CompareNullableCase(first = null, second = null, nullsFirst = false, sortOrder = SortOrder.DESCENDING),
            )
        }

        @JvmStatic
        fun testSortNullable(): List<SortNullableCase> {
            return listOf(true, false)
                .zip(SortOrder.values())
                .map { SortNullableCase(it.second, it.first) }
        }
    }
}
