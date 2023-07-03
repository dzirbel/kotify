package com.dzirbel.kotify.util

import assertk.assertThat
import assertk.assertions.containsOnly
import org.junit.jupiter.api.Test

class MapExtensionsTest {
    @Test
    fun filterNotNullValues() {
        val map = mapOf("a" to 1, "b" to null, "c" to 2, "d" to null)
        assertThat(map.filterNotNullValues()).containsOnly("a" to 1, "c" to 2)
    }
}
