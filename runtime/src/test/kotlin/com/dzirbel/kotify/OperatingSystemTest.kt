package com.dzirbel.kotify

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class OperatingSystemTest {
    data class Case(val name: String, val expected: OperatingSystem?)

    @ParameterizedTest
    @MethodSource
    fun of(case: Case) {
        assertThat(OperatingSystem.of(case.name)).isEqualTo(case.expected)
    }

    companion object {
        @JvmStatic
        fun of(): List<Case> {
            return listOf(
                Case("Windows NT", OperatingSystem.WINDOWS),
                Case("Windows 95", OperatingSystem.WINDOWS),
                Case("Windows (unknown)", OperatingSystem.WINDOWS),
                Case("Mac OS X", OperatingSystem.MAC),
                Case("Linux", OperatingSystem.LINUX),
                Case("Android", null),
            )
        }
    }
}
