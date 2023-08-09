package com.dzirbel.kotify.util.collections

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.jupiter.api.Test

class AverageTest {
    @Test
    fun averageOrNullWithValues() {
        val avg = listOf(1, null, 2, 3, 4, 5, null, 5).averageBy { it * 1.5 }
        assertThat(avg).isEqualTo(5.0)
    }

    @Test
    fun averageOrNullWithNoValues() {
        val avg = listOf(1, null, 2, 3, 4, 5, null, 5).averageBy { null }
        assertThat(avg).isNull()
    }
}
