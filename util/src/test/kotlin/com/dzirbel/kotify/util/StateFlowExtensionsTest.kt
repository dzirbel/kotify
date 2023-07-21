package com.dzirbel.kotify.util

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class StateFlowExtensionsTest {
    @Test
    fun `combineState combined stateFlow reflects updated value`() {
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
    fun `combineState multiple concurrent updates are still reflected`() {
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
    fun `combineState second round of collection persists values`() {
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

    @Test
    fun `mapIn initial value`() {
        val base = MutableStateFlow(1)
        runTest {
            val mappedValues = mutableListOf<Int>()
            val mapped = base.mapIn(scope = this) { x ->
                mappedValues.add(x)
                x * 2
            }

            // initial mapping takes place immediately
            assertThat(mappedValues).containsExactly(1)
            assertThat(mapped.value).isEqualTo(2)

            // starting collection does not re-map initial element
            runCurrent()

            assertThat(mappedValues).containsExactly(1)
            assertThat(mapped.value).isEqualTo(2)

            coroutineContext.cancelChildren() // cancel to stop mapIn collection
        }
    }

    @Test
    fun `mapIn updated value is reflected`() {
        val base = MutableStateFlow(0)
        runTest {
            base.value = 1 // emitted values before collection are ignored

            val mappedValues = mutableListOf<Int>()
            val mapped = base.mapIn(scope = this) { x ->
                mappedValues.add(x)
                x * 2
            }

            base.value = 2

            // mapping does not occur until collection is resumed
            assertThat(mappedValues).containsExactly(1)
            assertThat(mapped.value).isEqualTo(2)

            runCurrent()

            assertThat(mappedValues).containsExactly(1, 2)
            assertThat(mapped.value).isEqualTo(4)

            // assigning the same value does not trigger a mapping
            base.value = 2
            runCurrent()

            assertThat(mappedValues).containsExactly(1, 2)
            assertThat(mapped.value).isEqualTo(4)

            // multiple updates before collection skips any prior values
            base.value = 3
            base.value = 4

            runCurrent()

            assertThat(mappedValues).containsExactly(1, 2, 4)
            assertThat(mapped.value).isEqualTo(8)

            coroutineContext.cancelChildren() // cancel to stop mapIn collection
        }
    }

    @Test
    fun `mapIn with two collectors`() {
        val base = MutableStateFlow(1)
        runTest {
            val mappedValues1 = mutableListOf<Int>()
            val mappedValues2 = mutableListOf<Int>()
            val mapped1 = base.mapIn(scope = this) { x -> (x * 2).also { mappedValues1.add(x) } }
            val mapped2 = base.mapIn(scope = this) { x -> (x * 3).also { mappedValues2.add(x) } }

            assertThat(mappedValues1).containsExactly(1)
            assertThat(mappedValues2).containsExactly(1)
            assertThat(mapped1.value).isEqualTo(2)
            assertThat(mapped2.value).isEqualTo(3)

            base.value = 2

            assertThat(mappedValues1).containsExactly(1)
            assertThat(mappedValues2).containsExactly(1)
            assertThat(mapped1.value).isEqualTo(2)
            assertThat(mapped2.value).isEqualTo(3)

            runCurrent()

            assertThat(mappedValues1).containsExactly(1, 2)
            assertThat(mappedValues2).containsExactly(1, 2)
            assertThat(mapped1.value).isEqualTo(4)
            assertThat(mapped2.value).isEqualTo(6)

            coroutineContext.cancelChildren() // cancel to stop mapIn collection
        }
    }

    @Test
    fun `flatMapLatestIn initial value`() {
        val base = MutableStateFlow(1)
        runTest {
            val mappedValues = mutableListOf<Int>()
            val mapped = base.flatMapLatestIn(scope = this) { x ->
                mappedValues.add(x)
                MutableStateFlow(x * 2)
            }

            // initial mapping takes place immediately
            assertThat(mappedValues).containsExactly(1)
            assertThat(mapped.value).isEqualTo(2)

            // starting collection does not re-map initial element
            runCurrent()

            assertThat(mappedValues).containsExactly(1)
            assertThat(mapped.value).isEqualTo(2)

            coroutineContext.cancelChildren() // cancel to stop mapIn collection
        }
    }

    @Test
    fun `flatMapLatestIn updated value is reflected`() {
        val base = MutableStateFlow(0)
        runTest {
            base.value = 1 // emitted values before collection are ignored

            val mappedValues = mutableListOf<Int>()
            val mapped = base.flatMapLatestIn(scope = this) { x ->
                mappedValues.add(x)
                MutableStateFlow(x * 2)
            }

            base.value = 2

            // mapping does not occur until collection is resumed
            assertThat(mappedValues).containsExactly(1)
            assertThat(mapped.value).isEqualTo(2)

            runCurrent()

            assertThat(mappedValues).containsExactly(1, 2)
            assertThat(mapped.value).isEqualTo(4)

            // assigning the same value does not trigger a mapping
            base.value = 2
            runCurrent()

            assertThat(mappedValues).containsExactly(1, 2)
            assertThat(mapped.value).isEqualTo(4)

            // multiple updates before collection skips any prior values
            base.value = 3
            base.value = 4

            runCurrent()

            assertThat(mappedValues).containsExactly(1, 2, 4)
            assertThat(mapped.value).isEqualTo(8)

            coroutineContext.cancelChildren() // cancel to stop mapIn collection
        }
    }

    @Test
    fun `flatMapLatestIn updated downstream flow value is reflected`() {
        val base = MutableStateFlow(0)
        runTest {
            base.value = 1 // emitted values before collection are ignored

            val mappedValues = mutableListOf<Int>()
            val downstreamStateFlows = mutableListOf<MutableStateFlow<Int>>()
            val mapped = base.flatMapLatestIn(scope = this) { x ->
                mappedValues.add(x)
                MutableStateFlow(x * 2).also { downstreamStateFlows.add(it) }
            }

            base.value = 2
            runCurrent()
            base.value = 3
            runCurrent()

            assertThat(mappedValues).containsExactly(1, 2, 3)
            assertThat(mapped.value).isEqualTo(6)

            // updating a non-latest downstream flow does not affect the flatMapped flow
            downstreamStateFlows.first().value = 4
            assertThat(mappedValues).containsExactly(1, 2, 3)
            assertThat(mapped.value).isEqualTo(6)
            runCurrent()
            assertThat(mappedValues).containsExactly(1, 2, 3)
            assertThat(mapped.value).isEqualTo(6)

            // updating the latest downstream flow does affect the flatMapped flow
            downstreamStateFlows.last().value = 5
            assertThat(mappedValues).containsExactly(1, 2, 3)
            assertThat(mapped.value).isEqualTo(6)
            runCurrent()
            assertThat(mappedValues).containsExactly(1, 2, 3)
            assertThat(mapped.value).isEqualTo(5)

            coroutineContext.cancelChildren() // cancel to stop mapIn collection
        }
    }

    @Test
    fun `flatMapLatestIn with two collectors`() {
        val base = MutableStateFlow(1)
        runTest {
            val mappedValues1 = mutableListOf<Int>()
            val mappedValues2 = mutableListOf<Int>()
            val downstreamStateFlows1 = mutableListOf<MutableStateFlow<Int>>()
            val downstreamStateFlows2 = mutableListOf<MutableStateFlow<Int>>()
            val mapped1 = base.flatMapLatestIn(scope = this) { x ->
                mappedValues1.add(x)
                MutableStateFlow(x * 2).also { downstreamStateFlows1.add(it) }
            }
            val mapped2 = base.flatMapLatestIn(scope = this) { x ->
                mappedValues2.add(x)
                MutableStateFlow(x * 3).also { downstreamStateFlows2.add(it) }
            }

            assertThat(mappedValues1).containsExactly(1)
            assertThat(mappedValues2).containsExactly(1)
            assertThat(mapped1.value).isEqualTo(2)
            assertThat(mapped2.value).isEqualTo(3)

            base.value = 2

            assertThat(mappedValues1).containsExactly(1)
            assertThat(mappedValues2).containsExactly(1)
            assertThat(mapped1.value).isEqualTo(2)
            assertThat(mapped2.value).isEqualTo(3)

            runCurrent()

            assertThat(mappedValues1).containsExactly(1, 2)
            assertThat(mappedValues2).containsExactly(1, 2)
            assertThat(mapped1.value).isEqualTo(4)
            assertThat(mapped2.value).isEqualTo(6)

            coroutineContext.cancelChildren() // cancel to stop mapIn collection
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
