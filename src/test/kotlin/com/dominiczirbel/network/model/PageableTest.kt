package com.dominiczirbel.network.model

import com.google.common.truth.Truth.assertThat
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
                return prev!!
            }
        }
    }

    @Test
    fun fetchAllEmpty() {
        val pageable = CustomPageable.from(listOf<String>())
        assertThat(pageable.fetchAll()).isEmpty()
    }

    @Test
    fun fetchAll() {
        val pageable = CustomPageable.from(listOf("a", "b"), listOf("c"), listOf(), listOf("d"), listOf())
        assertThat(pageable.fetchAll()).containsExactly("a", "b", "c", "d")
    }
}
