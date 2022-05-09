package com.dzirbel.kotify.network.model

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class PageableTest {
    /** A simple [Pageable] with a pre-defined [next], for ease of testing. */
    data class CustomPageable<T>(
        override val items: List<T>,
        override val next: String?,
        val nextPageable: CustomPageable<T>?,
    ) : Pageable<T>() {
        fun fetchAll(): List<T> {
            var paging: CustomPageable<T>? = this
            return runBlocking {
                asFlow { url ->
                    assertThat(url).isEqualTo(paging?.next)
                    paging = paging?.nextPageable
                    paging
                }.toList()
            }
        }

        companion object {
            fun <T> from(vararg allItems: List<T>): CustomPageable<T> {
                var prev: CustomPageable<T>? = null
                for ((index, items) in allItems.withIndex().reversed()) {
                    prev = CustomPageable(items = items, next = index.toString(), nextPageable = prev)
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
