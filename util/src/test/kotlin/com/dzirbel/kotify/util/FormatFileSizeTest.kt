package com.dzirbel.kotify.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class FormatFileSizeTest {
    data class Case(val bytes: Long, val expected: String)

    @ParameterizedTest
    @MethodSource
    fun test(case: Case) {
        assertThat(formatFileSize(case.bytes)).isEqualTo(case.expected)
    }

    companion object {
        @JvmStatic
        fun test(): List<Case> {
            return listOf(
                Case(bytes = 0, expected = "0 bytes"),
                Case(bytes = 1, expected = "1 bytes"),
                Case(bytes = 2, expected = "2 bytes"),
                Case(bytes = 42, expected = "42 bytes"),
                Case(bytes = 1_000, expected = "1000 bytes"),
                Case(bytes = 1_001, expected = "1001 bytes"),
                Case(bytes = 1_023, expected = "1023 bytes"),
                Case(bytes = 1_024, expected = "1 kB"),
                Case(bytes = 1_025, expected = "1 kB"),
                Case(bytes = 1_200, expected = "1 kB"),
                Case(bytes = 2_000, expected = "2 kB"),
                Case(bytes = 12_345, expected = "12 kB"),
                Case(bytes = 123_456, expected = "121 kB"),
                Case(bytes = 456_789, expected = "446 kB"),
                Case(bytes = 1_000_000, expected = "977 kB"),
                Case(bytes = 1_048_576, expected = "1.0 MB"),
                Case(bytes = 2_500_000, expected = "2.4 MB"),
                Case(bytes = 23_456_000, expected = "22.4 MB"),
                Case(bytes = 1_073_741_823, expected = "1024.0 MB"),
                Case(bytes = 1_073_741_824, expected = "1.0 GB"),
                Case(bytes = 1_500_000_000, expected = "1.4 GB"),
            )
        }
    }
}
