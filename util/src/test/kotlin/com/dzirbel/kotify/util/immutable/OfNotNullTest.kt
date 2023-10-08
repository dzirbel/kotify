package com.dzirbel.kotify.util.immutable

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test

class OfNotNullTest {
    @Test
    fun `single not null`() {
        assertThat(persistentListOfNotNull(1)).isEqualTo(persistentListOf(1))
    }

    @Test
    fun `single null`() {
        assertThat(persistentListOfNotNull(null)).isEqualTo(persistentListOf())
    }

    @Test
    fun `multiple elements`() {
        assertThat(persistentListOfNotNull(1, null, 2, null, 3)).isEqualTo(persistentListOf(1, 2, 3))
    }
}
