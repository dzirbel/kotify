package com.dzirbel.kotify.util.coroutines

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class CombineTest {
    @Test
    fun `combineState combined stateFlow reflects updated value`() {
        runTest {
            val stateFlow1 = MutableStateFlow("1")
            val stateFlow2 = MutableStateFlow("2")
            val stateFlow3 = MutableStateFlow("3")
            val stateFlows = listOf(stateFlow1, stateFlow2, stateFlow3)

            val combinedArrays = mutableListOf<List<String>>()
            val combined = stateFlows.combineState { array ->
                combinedArrays.add(array.toList())
                array.joinToString()
            }

            assertThat(combinedArrays).containsExactly(listOf("1", "2", "3"))
            assertThat(combined.value).isEqualTo("1, 2, 3")

            collecting(combined) {
                assertThat(combinedArrays).containsExactly(listOf("1", "2", "3"))
                assertThat(combined.value).isEqualTo("1, 2, 3")

                runCurrent()

                assertThat(combinedArrays).containsExactly(
                    listOf("1", "2", "3"),
                    listOf("1", "2", "3"),
                )
                assertThat(combined.value).isEqualTo("1, 2, 3")

                stateFlow1.value = "1a"

                assertThat(combinedArrays).containsExactly(
                    listOf("1", "2", "3"),
                    listOf("1", "2", "3"),
                )
                assertThat(combined.value).isEqualTo("1, 2, 3")

                runCurrent()

                assertThat(combinedArrays).containsExactly(
                    listOf("1", "2", "3"),
                    listOf("1", "2", "3"),
                    listOf("1a", "2", "3"),
                )
                assertThat(combined.value).isEqualTo("1a, 2, 3")
            }
        }
    }

    @Test
    fun `combineState multiple concurrent updates are still reflected`() {
        runTest {
            val stateFlow1 = MutableStateFlow("1")
            val stateFlow2 = MutableStateFlow("2")
            val stateFlow3 = MutableStateFlow("3")
            val stateFlows = listOf(stateFlow1, stateFlow2, stateFlow3)

            val combinedArrays = mutableListOf<List<String>>()
            val combined = stateFlows.combineState { array ->
                combinedArrays.add(array.toList())
                array.joinToString()
            }

            collecting(combined) {
                assertThat(combinedArrays).containsExactly(listOf("1", "2", "3"))
                assertThat(combined.value).isEqualTo("1, 2, 3")

                stateFlow1.value = "1a"
                stateFlow2.value = "2a"

                assertThat(combinedArrays).containsExactly(listOf("1", "2", "3"))
                assertThat(combined.value).isEqualTo("1, 2, 3")

                runCurrent()

                assertThat(combinedArrays).containsExactly(listOf("1", "2", "3"), listOf("1a", "2a", "3"))
                assertThat(combined.value).isEqualTo("1a, 2a, 3")
            }
        }
    }

    @Test
    fun `combineState second round of collection persists values`() {
        runTest {
            val stateFlow1 = MutableStateFlow("1")
            val stateFlow2 = MutableStateFlow("2")
            val stateFlow3 = MutableStateFlow("3")
            val stateFlows = listOf(stateFlow1, stateFlow2, stateFlow3)

            val combinedArrays = mutableListOf<List<String>>()
            val combined = stateFlows.combineState { array ->
                combinedArrays.add(array.toList())
                array.joinToString()
            }

            collecting(combined) {
                assertThat(combinedArrays).containsExactly(listOf("1", "2", "3"))
                assertThat(combined.value).isEqualTo("1, 2, 3")

                stateFlow1.value = "1a"

                assertThat(combinedArrays).containsExactly(listOf("1", "2", "3"))
                assertThat(combined.value).isEqualTo("1, 2, 3")

                runCurrent()

                assertThat(combinedArrays).containsExactly(listOf("1", "2", "3"), listOf("1a", "2", "3"))
                assertThat(combined.value).isEqualTo("1a, 2, 3")
            }

            assertThat(stateFlow1.value).isEqualTo("1a")
            assertThat(combined.value).isEqualTo("1a, 2, 3")

            collecting(combined) {
                assertThat(combinedArrays).containsExactly(listOf("1", "2", "3"), listOf("1a", "2", "3"))
                assertThat(combined.value).isEqualTo("1a, 2, 3")

                stateFlow3.value = "3a"

                assertThat(combinedArrays).containsExactly(listOf("1", "2", "3"), listOf("1a", "2", "3"))
                assertThat(combined.value).isEqualTo("1a, 2, 3")

                runCurrent()

                assertThat(combinedArrays)
                    .containsExactly(listOf("1", "2", "3"), listOf("1a", "2", "3"), listOf("1a", "2", "3a"))
                assertThat(combined.value).isEqualTo("1a, 2, 3a")
            }
        }
    }

    /**
     * Asynchronously collects [flow] in [block], then cancels collection.
     */
    private fun CoroutineScope.collecting(flow: Flow<*>, block: () -> Unit) {
        val job = launch { flow.collect() }
        block()
        job.cancel()
    }
}
