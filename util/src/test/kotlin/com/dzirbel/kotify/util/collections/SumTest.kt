package com.dzirbel.kotify.util.collections

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class SumTest {
    @Test
    fun sumOfNullable() {
        val list = List(10) { it + 1 }
        val mapper: (Int) -> Float? = { if (it % 2 == 0) (it * 2).toFloat() else null }

        assertThat(list.sumOf(mapper)).isEqualTo(list.mapNotNull(mapper).sum())
    }
}
