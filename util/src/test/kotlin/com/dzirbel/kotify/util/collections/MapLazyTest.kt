package com.dzirbel.kotify.util.collections

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class MapLazyTest {
    @Test
    fun test() {
        val list = listOf("a", "b", "c")
        val mapped = mutableListOf<String>()

        val iterator = list
            .mapLazy { s ->
                mapped.add(s)
                "${s}1"
            }
            .iterator()

        assertThat(mapped).isEmpty()

        assertThat(iterator.hasNext()).isTrue()
        assertThat(mapped).isEmpty()
        assertThat(iterator.next()).isEqualTo("a1")
        assertThat(mapped).containsExactly("a")

        assertThat(iterator.hasNext()).isTrue()
        assertThat(mapped).containsExactly("a")
        assertThat(iterator.next()).isEqualTo("b1")
        assertThat(mapped).containsExactly("a", "b")

        assertThat(iterator.hasNext()).isTrue()
        assertThat(mapped).containsExactly("a", "b")
        assertThat(iterator.next()).isEqualTo("c1")
        assertThat(mapped).containsExactly("a", "b", "c")

        assertThat(iterator.hasNext()).isFalse()
    }
}
