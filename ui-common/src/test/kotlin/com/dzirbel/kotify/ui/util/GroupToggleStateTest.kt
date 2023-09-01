package com.dzirbel.kotify.ui.util

import androidx.compose.ui.state.ToggleableState
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class GroupToggleStateTest {
    @Test
    fun test() {
        assertThat(listOf(1, 2, 3).groupToggleState(setOf(1, 2, 3))).isEqualTo(ToggleableState.On)
        assertThat(listOf(1, 2, 3).groupToggleState(setOf(1, 2))).isEqualTo(ToggleableState.Indeterminate)
        assertThat(listOf(1, 2, 3).groupToggleState(setOf(2, 3))).isEqualTo(ToggleableState.Indeterminate)
        assertThat(listOf(1, 2, 3).groupToggleState(setOf(1, 3))).isEqualTo(ToggleableState.Indeterminate)
        assertThat(listOf(1, 2, 3).groupToggleState(setOf(2))).isEqualTo(ToggleableState.Indeterminate)
        assertThat(listOf(1, 2, 3).groupToggleState(emptySet())).isEqualTo(ToggleableState.Off)

        // empty list is always on
        assertThat(emptyList<Int>().groupToggleState(emptySet())).isEqualTo(ToggleableState.On)
        assertThat(emptyList<Int>().groupToggleState(setOf(1, 2, 3))).isEqualTo(ToggleableState.On)

        // extra enabled values are ignored
        assertThat(listOf(1, 2, 3).groupToggleState(setOf(1, 2, 3, 4, 5))).isEqualTo(ToggleableState.On)
        assertThat(listOf(1, 2, 3).groupToggleState(setOf(4, 5))).isEqualTo(ToggleableState.Off)
        assertThat(listOf(1, 2, 3).groupToggleState(setOf(2, 4, 5))).isEqualTo(ToggleableState.Indeterminate)
    }
}
