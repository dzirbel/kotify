package com.dzirbel.kotify.repository.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.math.sign

internal class ReleaseDateTest {
    data class ParseCase(val releaseDateString: String, val expected: ReleaseDate?)
    data class CompareCase(val first: ReleaseDate, val second: ReleaseDate, val comparison: Int)
    data class ToStringCase(val releaseDate: ReleaseDate, val expected: String)

    @ParameterizedTest
    @MethodSource
    fun testParse(case: ParseCase) {
        assertThat(ReleaseDate.parse(case.releaseDateString)).isEqualTo(case.expected)
    }

    @ParameterizedTest
    @MethodSource
    fun testCompare(case: CompareCase) {
        assertThat(case.first.compareTo(case.second).sign).isEqualTo(case.comparison.sign)
        assertThat(case.second.compareTo(case.first).sign).isEqualTo(-case.comparison.sign)
    }

    @ParameterizedTest
    @MethodSource
    fun testToString(case: ToStringCase) {
        assertThat(case.releaseDate.toString()).isEqualTo(case.expected)
    }

    companion object {
        @JvmStatic
        fun testParse(): List<ParseCase> {
            return listOf(
                ParseCase("1995", ReleaseDate(year = 1995, month = null, day = null)),
                ParseCase("1995-08", ReleaseDate(year = 1995, month = 8, day = null)),
                ParseCase("1995-08-13", ReleaseDate(year = 1995, month = 8, day = 13)),
                ParseCase("abcd", null),
                ParseCase("42", null),
                ParseCase("2022-ab-cd", null),
            )
        }

        @JvmStatic
        fun testCompare(): List<CompareCase> {
            return listOf(
                CompareCase(first = ReleaseDate(1995, 8, 13), second = ReleaseDate(1995, 8, 13), comparison = 0),
                CompareCase(first = ReleaseDate(1995, 8, 14), second = ReleaseDate(1995, 8, 13), comparison = 1),
                CompareCase(first = ReleaseDate(1995, 9, 12), second = ReleaseDate(1995, 8, 13), comparison = 1),
                CompareCase(first = ReleaseDate(1996, 1, 20), second = ReleaseDate(1995, 8, 13), comparison = 1),
                CompareCase(first = ReleaseDate(1995, 8, 13), second = ReleaseDate(1995, null, null), comparison = 0),
                CompareCase(first = ReleaseDate(1995, 8, 13), second = ReleaseDate(1995, 8, null), comparison = 0),
            )
        }

        @JvmStatic
        fun testToString(): List<ToStringCase> {
            return listOf(
                ToStringCase(releaseDate = ReleaseDate(2022, 1, 1), expected = "2022-01-01"),
                ToStringCase(releaseDate = ReleaseDate(2022, 11, 12), expected = "2022-11-12"),
                ToStringCase(releaseDate = ReleaseDate(2022, 1, null), expected = "2022-01"),
                ToStringCase(releaseDate = ReleaseDate(2022, null, null), expected = "2022"),
            )
        }
    }
}
