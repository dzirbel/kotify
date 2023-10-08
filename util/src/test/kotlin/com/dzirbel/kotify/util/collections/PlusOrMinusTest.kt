package com.dzirbel.kotify.util.collections

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import kotlinx.collections.immutable.persistentSetOf
import org.junit.jupiter.api.Test

class PlusOrMinusTest {
    @Test
    fun `plus element to plain Set`() {
        var set = setOf("a", "b", "c")
        set = set.plusOrMinus(value = "d", condition = true)
        assertThat(set).containsExactlyInAnyOrder("a", "b", "c", "d")
    }

    @Test
    fun `plus element to PersistentSet`() {
        var set = persistentSetOf("a", "b", "c")
        set = set.plusOrMinus(value = "d", condition = true)
        assertThat(set).containsExactlyInAnyOrder("a", "b", "c", "d")
    }

    @Test
    fun `minus element from plain Set`() {
        var set = setOf("a", "b", "c")
        set = set.plusOrMinus(value = "c", condition = false)
        assertThat(set).containsExactlyInAnyOrder("a", "b")
    }

    @Test
    fun `minus element from PersistentSet`() {
        var set = persistentSetOf("a", "b", "c")
        set = set.plusOrMinus(value = "c", condition = false)
        assertThat(set).containsExactlyInAnyOrder("a", "b")
    }
}
