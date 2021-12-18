package com.dzirbel.kotify.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource

internal class ComparableExtensionsTest {
    data class CompareToNullableCase(val first: String?, val second: String?, val nullsFirst: Boolean)

    @ParameterizedTest
    @MethodSource
    fun testCompareToNullable(case: CompareToNullableCase) {
        if (case.first == case.second) {
            assertThat(case.first.compareToNullable(case.second, nullsFirst = case.nullsFirst)).isEqualTo(0)
            assertThat(case.second.compareToNullable(case.first, nullsFirst = case.nullsFirst)).isEqualTo(0)
        } else {
            assertThat(case.first.compareToNullable(case.second, nullsFirst = case.nullsFirst)).isLessThan(0)
            assertThat(case.second.compareToNullable(case.first, nullsFirst = case.nullsFirst)).isGreaterThan(0)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testSortNullable(nullsFirst: Boolean) {
        val list = listOf(null, "b", null, "c", null, "a", null)
            .sortedWith { o1, o2 -> o1.compareToNullable(o2, nullsFirst = nullsFirst) }

        val expected = if (nullsFirst) {
            listOf(null, null, null, null, "a", "b", "c")
        } else {
            listOf("a", "b", "c", null, null, null, null)
        }

        assertThat(list).isEqualTo(expected)
    }

    companion object {
        @JvmStatic
        fun testCompareToNullable(): List<CompareToNullableCase> {
            return listOf(
                CompareToNullableCase(first = "a", second = "a", nullsFirst = true),
                CompareToNullableCase(first = "a", second = "a", nullsFirst = false),

                CompareToNullableCase(first = "a", second = "b", nullsFirst = false),
                CompareToNullableCase(first = "a", second = "b", nullsFirst = true),

                CompareToNullableCase(first = "a", second = null, nullsFirst = false),
                CompareToNullableCase(first = null, second = "a", nullsFirst = true),

                CompareToNullableCase(first = null, second = null, nullsFirst = true),
                CompareToNullableCase(first = null, second = null, nullsFirst = false),
            )
        }
    }
}
