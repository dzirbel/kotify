package com.dzirbel.kotify.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class FormatWithUnitTest {
    @Test
    fun formattedWithUnit() {
        assertThat(0.formattedWithUnit("unit")).isEqualTo("0 units")
        assertThat(1.formattedWithUnit("unit")).isEqualTo("1 unit")
        assertThat(2.formattedWithUnit("unit")).isEqualTo("2 units")
        assertThat((-1).formattedWithUnit("unit")).isEqualTo("-1 units")

        assertThat(0L.formattedWithUnit("unit")).isEqualTo("0 units")
        assertThat(1L.formattedWithUnit("unit")).isEqualTo("1 unit")
        assertThat(2L.formattedWithUnit("unit")).isEqualTo("2 units")
        assertThat((-1L).formattedWithUnit("unit")).isEqualTo("-1 units")
    }
}
