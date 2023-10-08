package com.dzirbel.kotify.util.collections

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import org.junit.jupiter.api.Test

class MapIfAllNotNullTest {
    @Test
    fun `all not null`() {
        val array = arrayOf("a", "b", "c")
        val mapped = mutableListOf<String>()

        val result = array.mapIfAllNotNull { s ->
            mapped.add(s)
            "${s}1"
        }

        assertThat(result).isNotNull().containsExactly("a1", "b1", "c1")
        assertThat(mapped).containsExactly("a", "b", "c")
    }

    @Test
    fun `some null`() {
        val array = arrayOf("a", "b", "c")
        val mapped = mutableListOf<String>()

        val result = array.mapIfAllNotNull { s ->
            mapped.add(s)
            if (s.contains("b")) null else "${s}1"
        }

        assertThat(result).isNull()
        assertThat(mapped).containsExactly("a", "b")
    }
}
