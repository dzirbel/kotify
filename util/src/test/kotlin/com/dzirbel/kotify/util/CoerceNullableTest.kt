package com.dzirbel.kotify.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class CoerceNullableTest {
    @Test
    fun coerceAtMostNullable() {
        assertThat(2.coerceAtMostNullable(3)).isEqualTo(2)
        assertThat(2.coerceAtMostNullable(1)).isEqualTo(1)
        assertThat(2.coerceAtMostNullable(null)).isEqualTo(2)
    }

    @Test
    fun coerceAtLeastNullable() {
        assertThat(2.coerceAtLeastNullable(3)).isEqualTo(3)
        assertThat(2.coerceAtLeastNullable(1)).isEqualTo(2)
        assertThat(2.coerceAtLeastNullable(null)).isEqualTo(2)
    }
}
