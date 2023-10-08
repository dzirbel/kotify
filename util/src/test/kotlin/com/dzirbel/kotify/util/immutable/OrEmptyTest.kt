package com.dzirbel.kotify.util.immutable

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSameAs
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test

class OrEmptyTest {
    @Test
    fun `not null`() {
        val list = persistentListOf(1, 2, 3)
        assertThat(list.orEmpty()).isSameAs(list)
    }

    @Test
    fun `null`() {
        val list: PersistentList<Int>? = null
        assertThat(list.orEmpty()).isEqualTo(persistentListOf())
    }
}
