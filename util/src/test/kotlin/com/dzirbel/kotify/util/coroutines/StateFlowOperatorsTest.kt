package com.dzirbel.kotify.util.coroutines

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class StateFlowOperatorsTest {
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

            coroutineContext.cancelChildren() // cancel to stop flatMapLatestIn collection
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

            coroutineContext.cancelChildren() // cancel to stop flatMapLatestIn collection
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

            coroutineContext.cancelChildren() // cancel to stop flatMapLatestIn collection
        }
    }

    @Test
    fun `flatMapLatestIn initial updated downstream flow value is reflected`() {
        val base = MutableStateFlow(1)
        runTest {
            val mappedValues = mutableListOf<Int>()
            var downstreamStateFlow: MutableStateFlow<Int>? = null
            val mapped = base.flatMapLatestIn(scope = this) { x ->
                mappedValues.add(x)
                MutableStateFlow(x * 2).also { downstreamStateFlow = it }
            }

            runCurrent()

            assertThat(mappedValues).containsExactly(1)
            assertThat(mapped.value).isEqualTo(2)

            // updating the latest downstream flow affects the flatMapped flow
            downstreamStateFlow!!.value = 5
            assertThat(mappedValues).containsExactly(1)
            assertThat(mapped.value).isEqualTo(2)
            runCurrent()
            assertThat(mappedValues).containsExactly(1)
            assertThat(mapped.value).isEqualTo(5)

            coroutineContext.cancelChildren() // cancel to stop flatMapLatestIn collection
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

            coroutineContext.cancelChildren() // cancel to stop flatMapLatestIn collection
        }
    }

    @Test
    fun `onEachIn initial value`() {
        runTest {
            val seenValues = mutableListOf<Int>()
            val flow = MutableStateFlow(1)
                .onEachIn(scope = this) { seenValues.add(it) }

            // initial action takes place immediately
            assertThat(seenValues).containsExactly(1)
            assertThat(flow.value).isEqualTo(1)

            // starting collection does not re-invoke action on the initial element
            runCurrent()

            assertThat(seenValues).containsExactly(1)
            assertThat(flow.value).isEqualTo(1)

            coroutineContext.cancelChildren() // cancel to stop onEachIn collection
        }
    }

    @Test
    fun `onEachIn updated value is reflected`() {
        val base = MutableStateFlow(0)
        runTest {
            base.value = 1 // emitted values before collection are ignored

            val seenValues = mutableListOf<Int>()
            val flow = base
                .onEachIn(scope = this) { seenValues.add(it) }

            base.value = 2

            // action does not occur until collection is resumed
            assertThat(seenValues).containsExactly(1)
            assertThat(flow.value).isEqualTo(1)

            runCurrent()

            assertThat(seenValues).containsExactly(1, 2)
            assertThat(flow.value).isEqualTo(2)

            // assigning the same value does not trigger a mapping
            base.value = 2
            runCurrent()

            assertThat(seenValues).containsExactly(1, 2)
            assertThat(flow.value).isEqualTo(2)

            // multiple updates before collection skips any prior values
            base.value = 3
            base.value = 4

            runCurrent()

            assertThat(seenValues).containsExactly(1, 2, 4)
            assertThat(flow.value).isEqualTo(4)

            coroutineContext.cancelChildren() // cancel to stop onEachIn collection
        }
    }

    @Test
    fun `onEachIn with two collectors`() {
        val base = MutableStateFlow(1)
        runTest {
            val seenValues1 = mutableListOf<Int>()
            val seenValues2 = mutableListOf<Int>()
            val flow1 = base.onEachIn(scope = this) { seenValues1.add(it) }
            val flow2 = base.onEachIn(scope = this) { seenValues2.add(it) }

            assertThat(seenValues1).containsExactly(1)
            assertThat(seenValues2).containsExactly(1)
            assertThat(flow1.value).isEqualTo(1)
            assertThat(flow2.value).isEqualTo(1)

            base.value = 2

            assertThat(seenValues1).containsExactly(1)
            assertThat(seenValues2).containsExactly(1)
            assertThat(flow1.value).isEqualTo(1)
            assertThat(flow2.value).isEqualTo(1)

            runCurrent()

            assertThat(seenValues1).containsExactly(1, 2)
            assertThat(seenValues2).containsExactly(1, 2)
            assertThat(flow1.value).isEqualTo(2)
            assertThat(flow2.value).isEqualTo(2)

            coroutineContext.cancelChildren() // cancel to stop onEachIn collection
        }
    }
}
