package com.dzirbel.kotify.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class StateFlowExtensionsTest {
    @Test
    fun `combined stateFlow reflects updated value`() {
        runTest {
            val stateFlow1 = MutableStateFlow("1")
            val stateFlow2 = MutableStateFlow("2")
            val stateFlow3 = MutableStateFlow("3")
            val stateFlows = listOf(stateFlow1, stateFlow2, stateFlow3)

            val combined = stateFlows.combineState { it.joinToString() }

            collecting(combined) {
                assertThat(combined.value).isEqualTo("1, 2, 3")

                stateFlow1.value = "1a"

                assertThat(combined.value).isEqualTo("1, 2, 3")

                runCurrent()

                assertThat(combined.value).isEqualTo("1a, 2, 3")
            }
        }
    }

    @Test
    fun `multiple concurrent updates are still reflected`() {
        runTest {
            val stateFlow1 = MutableStateFlow("1")
            val stateFlow2 = MutableStateFlow("2")
            val stateFlow3 = MutableStateFlow("3")
            val stateFlows = listOf(stateFlow1, stateFlow2, stateFlow3)

            val combined = stateFlows.combineState { it.joinToString() }

            collecting(combined) {
                assertThat(combined.value).isEqualTo("1, 2, 3")

                stateFlow1.value = "1a"
                stateFlow2.value = "2a"

                assertThat(combined.value).isEqualTo("1, 2, 3")

                runCurrent()

                assertThat(combined.value).isEqualTo("1a, 2a, 3")
            }
        }
    }

    @Test
    fun `second round of collection persists values`() {
        runTest {
            val stateFlow1 = MutableStateFlow("1")
            val stateFlow2 = MutableStateFlow("2")
            val stateFlow3 = MutableStateFlow("3")
            val stateFlows = listOf(stateFlow1, stateFlow2, stateFlow3)

            val combined = stateFlows.combineState { it.joinToString() }

            collecting(combined) {
                assertThat(combined.value).isEqualTo("1, 2, 3")

                stateFlow1.value = "1a"

                assertThat(combined.value).isEqualTo("1, 2, 3")

                runCurrent()

                assertThat(combined.value).isEqualTo("1a, 2, 3")
            }

            assertThat(stateFlow1.value).isEqualTo("1a")
            assertThat(combined.value).isEqualTo("1a, 2, 3")

            collecting(combined) {
                assertThat(combined.value).isEqualTo("1a, 2, 3")

                stateFlow3.value = "3a"

                assertThat(combined.value).isEqualTo("1a, 2, 3")

                runCurrent()

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
