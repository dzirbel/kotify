package com.dzirbel.kotify.util

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import org.junit.jupiter.api.Test

class SetExtensionsTest {
    @Test
    fun plusSingleElement() {
        var set = setOf("a", "b", "c")
        set = set.plusOrMinus(value = "d", condition = true)
        assertThat(set).containsExactlyInAnyOrder("a", "b", "c", "d")
    }

    @Test
    fun plusMultipleElement() {
        var set = setOf("a", "b", "c")
        set = set.plusOrMinus(elements = listOf("c", "d", "e"), condition = true)
        assertThat(set).containsExactlyInAnyOrder("a", "b", "c", "d", "e")
    }

    @Test
    fun minusSingleElement() {
        var set = setOf("a", "b", "c")
        set = set.plusOrMinus(value = "c", condition = false)
        assertThat(set).containsExactlyInAnyOrder("a", "b")
    }

    @Test
    fun minusMultipleElement() {
        var set = setOf("a", "b", "c")
        set = set.plusOrMinus(elements = listOf("b", "c", "d"), condition = false)
        assertThat(set).containsExactlyInAnyOrder("a")
    }
}
