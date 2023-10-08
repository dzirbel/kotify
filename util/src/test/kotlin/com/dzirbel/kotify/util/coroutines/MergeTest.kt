package com.dzirbel.kotify.util.coroutines

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class MergeTest {
    @Test
    fun mergeFlows() {
        runTest {
            val emissions = listOf(1, 2, 3)
                .mergeFlows { n ->
                    val char = 'a' + (n - 1)
                    flow {
                        repeat(n) { i ->
                            emit("$char${i + 1}")
                        }
                    }
                }
                .toList()

            assertThat(emissions).containsExactlyInAnyOrder("a1", "b1", "b2", "c1", "c2", "c3")
        }
    }
}
