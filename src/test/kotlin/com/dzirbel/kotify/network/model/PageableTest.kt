package com.dzirbel.kotify.network.model

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import org.junit.jupiter.api.Test

internal class PageableTest {
    /** A simple [Pageable] with a pre-defined [next], for ease of testing. */
    data class CustomPageable<T>(override val items: List<T>, val next: CustomPageable<T>?) : Pageable<T>() {
        override val hasNext = next != null

        fun fetchAll(): List<T> {
            return fetchAll<CustomPageable<T>> { it.next }
        }

        companion object {
            fun <T> from(vararg allItems: List<T>): CustomPageable<T> {
                var prev: CustomPageable<T>? = null
                for (items in allItems.reversed()) {
                    prev = CustomPageable(items = items, next = prev)
                }
                return requireNotNull(prev)
            }
        }
    }

    @Test
    fun fetchAllEmpty() {
        val pageable = CustomPageable.from(emptyList<String>())
        assertThat(pageable.fetchAll()).isEmpty()
    }

    @Test
    fun fetchAll() {
        val pageable = CustomPageable.from(listOf("a", "b"), listOf("c"), emptyList(), listOf("d"), emptyList())
        assertThat(pageable.fetchAll()).containsExactly("a", "b", "c", "d")
    }
}
