package com.dzirbel.kotify.repository.util

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSameSizeAs
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isBetween
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThan
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import com.dzirbel.kotify.util.containsExactlyElementsOf
import com.dzirbel.kotify.util.containsExactlyElementsOfInAnyOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger

class SynchronizedWeakStateFlowMapTest {
    @Test
    fun `create new flow with null default`() {
        val map = SynchronizedWeakStateFlowMap<String, Int>()
        val onCreateValues = mutableListOf<Int?>()
        val onExistingValues = mutableListOf<Int?>()

        val stateFlow = map.getOrCreateStateFlow(
            key = "key",
            defaultValue = { null },
            onCreate = onCreateValues::add,
            onExisting = onExistingValues::add,
        )

        assertThat(stateFlow.value).isNull()
        assertThat(onCreateValues).containsExactly(null)
        assertThat(onExistingValues).isEmpty()
        assertThat(map.getValue("key")).isNull()
    }

    @Test
    fun `create new flow with non-null default`() {
        val default = 123
        val map = SynchronizedWeakStateFlowMap<String, Int>()
        val onCreateValues = mutableListOf<Int?>()
        val onExistingValues = mutableListOf<Int?>()

        val stateFlow = map.getOrCreateStateFlow(
            key = "key",
            defaultValue = { default },
            onCreate = onCreateValues::add,
            onExisting = onExistingValues::add,
        )

        assertThat(stateFlow.value).isEqualTo(default)
        assertThat(onCreateValues).containsExactly(default)
        assertThat(onExistingValues).isEmpty()
        assertThat(map.getValue("key")).isEqualTo(default)
    }

    @Test
    fun `subsequent calls to create new flow return existing instance`() {
        val map = SynchronizedWeakStateFlowMap<String, Int>()
        val onCreateValues = mutableListOf<Int?>()
        val onExistingValues = mutableListOf<Int?>()

        val stateFlow = map.getOrCreateStateFlow(
            key = "key",
            defaultValue = { null },
            onCreate = onCreateValues::add,
            onExisting = onExistingValues::add,
        )

        assertThat(stateFlow.value).isNull()
        assertThat(onCreateValues).containsExactly(null)
        assertThat(onExistingValues).isEmpty()
        assertThat(map.getValue("key")).isNull()

        val stateFlow2 = map.getOrCreateStateFlow(
            key = "key",
            defaultValue = { 123 },
            onCreate = onCreateValues::add,
            onExisting = onExistingValues::add,
        )

        assertThat(stateFlow2).isSameInstanceAs(stateFlow)
        assertThat(onCreateValues).containsExactly(null)
        assertThat(onExistingValues).containsExactly(null)
        assertThat(map.getValue("key")).isNull() // new default value is not used
    }

    @RepeatedTest(10)
    fun `creating flows in parallel only results in a single creation`() {
        val concurrency = 100 // number of parallel calls to make
        val map = SynchronizedWeakStateFlowMap<String, Int>()
        val onCreateValues = mutableListOf<Int?>()
        val onExistingValues = mutableListOf<Int?>()
        val stateFlows = mutableSetOf<StateFlow<Int?>>()
        var sequentialLaunches = 0 // fuzzy count of launches which were sequential

        runBlocking(Dispatchers.IO) {
            val jobs = List(concurrency) { i ->
                launch {
                    val launchesInit = sequentialLaunches
                    val stateFlow = map.getOrCreateStateFlow(
                        key = "key",
                        defaultValue = { i },
                        onCreate = { synchronized(onCreateValues) { onCreateValues.add(it) } },
                        onExisting = { synchronized(onExistingValues) { onExistingValues.add(it) } },
                    )

                    delay(1) // delay to ensure calls are made concurrently

                    synchronized(stateFlows) { stateFlows.add(stateFlow) }
                    sequentialLaunches = launchesInit + 1
                }
            }

            jobs.joinAll()
        }

        assertThat(stateFlows).hasSize(1) // all StateFlows should be the same instance

        val createdValue = stateFlows.first().value // created value is chosen arbitrarily from the launched jobs
        assertThat(createdValue).isNotNull().isBetween(0, concurrency)

        assertThat(onCreateValues).containsExactly(createdValue)
        assertThat(onExistingValues).containsExactlyElementsOf(List(concurrency - 1) { createdValue })
        assertThat(map.getValue("key")).isEqualTo(stateFlows.first().value)
        assertThat(sequentialLaunches).isLessThan(concurrency) // verify that launches did not happen concurrently
    }

    @Test
    fun `batch create creates only missing flows`() {
        val map = SynchronizedWeakStateFlowMap<String, Int>()

        val keys1 = listOf("a", "b", "c")
        val createdKeys1 = mutableSetOf<String>()
        val existingCounts1 = mutableListOf<Int>()

        val flows1 = map.getOrCreateStateFlows(
            keys = keys1,
            onCreate = { createdKeys1.addAll(it.keys) },
            onExisting = { existingCounts1.add(it) },
        )

        assertThat(flows1).hasSameSizeAs(keys1)
        assertThat(createdKeys1).containsExactlyElementsOfInAnyOrder(keys1)
        assertThat(existingCounts1).isEmpty()

        val keys2 = listOf("b", "d", "e", "c", "e")
        val createdKeys2 = mutableSetOf<String>()
        val existingCounts2 = mutableListOf<Int>()

        val flows2 = map.getOrCreateStateFlows(
            keys = keys2,
            onCreate = { createdKeys2.addAll(it.keys) },
            onExisting = { existingCounts2.add(it) },
        )

        assertThat(flows2).hasSameSizeAs(keys2)
        assertThat(createdKeys2).containsExactlyElementsOfInAnyOrder(keys2.minus(keys1).toSet())
        assertThat(existingCounts2).containsExactly(keys2.minus(keys1).size)
        assertThat(flows2).index(0).isSameInstanceAs(flows1[1]) // same flows for "b"
        assertThat(flows2).index(3).isSameInstanceAs(flows1[2]) // same flows for "c"
        assertThat(flows2).index(2).isSameInstanceAs(flows2[4]) // same flows for first and second "e"
    }

    @RepeatedTest(10)
    fun `batch creating flows in parallel only results in a single creation`() {
        val concurrency = 100 // number of parallel calls to make
        val keys = listOf("a", "b", "c")
        val map = SynchronizedWeakStateFlowMap<String, Int>()
        val onCreateCalls = AtomicInteger(0)
        val onExistingCalls = AtomicInteger(0)
        val stateFlows = mutableSetOf<StateFlow<Int?>>()
        var sequentialLaunches = 0 // fuzzy count of launches which were sequential

        runBlocking(Dispatchers.IO) {
            val jobs = List(concurrency) {
                launch {
                    val launchesInit = sequentialLaunches
                    val createdStateFlows = map.getOrCreateStateFlows(
                        keys = keys,
                        onCreate = { onCreateCalls.addAndGet(it.size) },
                        onExisting = { onExistingCalls.addAndGet(it) },
                    )

                    delay(1) // delay to ensure calls are made concurrently

                    synchronized(stateFlows) { stateFlows.addAll(createdStateFlows) }
                    sequentialLaunches = launchesInit + 1
                }
            }

            jobs.joinAll()
        }

        assertThat(stateFlows).hasSameSizeAs(keys) // all StateFlows should be the same instance per key
        assertThat(onCreateCalls.get()).isEqualTo(keys.size)
        assertThat(onExistingCalls.get()).isEqualTo((concurrency - 1) * keys.size)
        assertThat(sequentialLaunches).isLessThan(concurrency) // verify that launches did not happen concurrently
    }

    @Test
    fun `updated value is reflected`() {
        val map = SynchronizedWeakStateFlowMap<String, Int>()

        val stateFlow = map.getOrCreateStateFlow("key")

        assertThat(stateFlow.value).isNull()
        assertThat(map.getValue("key")).isNull()

        map.updateValue("key", 123)

        assertThat(stateFlow.value).isEqualTo(123)
        assertThat(map.getValue("key")).isEqualTo(123)
    }

    @Test
    fun `updated value is ignored for missing key`() {
        val map = SynchronizedWeakStateFlowMap<String, Int>()

        map.updateValue("key", 123)

        assertThat(map.getValue("key")).isNull()

        val stateFlow = map.getOrCreateStateFlow("key", defaultValue = { 456 })

        assertThat(stateFlow.value).isEqualTo(456)
        assertThat(map.getValue("key")).isEqualTo(456)
    }

    @Test
    fun `updated value computation is only called when value is present`() {
        val map = SynchronizedWeakStateFlowMap<String, Int>()
        var computationCalls = 0

        map.updateValue("key") { value ->
            computationCalls++
            value?.let { it + 1 }
        }

        assertThat(computationCalls).isEqualTo(0)

        val stateFlow = map.getOrCreateStateFlow("key", defaultValue = { 0 })

        map.updateValue("key") { value ->
            computationCalls++
            value?.let { it + 1 }
        }

        assertThat(computationCalls).isEqualTo(1)
        assertThat(map.getValue("key")).isEqualTo(1)
        assertThat(stateFlow.value).isEqualTo(1)
    }

    @Test
    fun `computeAll updates all values`() {
        val map = SynchronizedWeakStateFlowMap<String, Int>()
        val keys = listOf("1", "2", "a")
        val computedKeys = mutableSetOf<String>()

        map.getOrCreateStateFlows(keys)

        map.computeAll { key ->
            computedKeys.add(key)
            key.toIntOrNull()
        }

        assertThat(computedKeys).containsExactlyElementsOfInAnyOrder(keys)

        assertThat(map.getValue("1")).isEqualTo(1)
        assertThat(map.getValue("2")).isEqualTo(2)
        assertThat(map.getValue("a")).isNull()
    }

    @Test
    fun `clear removes all values`() {
        val map = SynchronizedWeakStateFlowMap<String, Int>()
        val keys = listOf("a", "b", "c")

        val stateFlows = map.getOrCreateStateFlows(keys)
        map.updateValue("a", 1)
        map.updateValue("b", 2)

        assertThat(map.getValue("a")).isNotNull()
        assertThat(map.getValue("b")).isNotNull()
        assertThat(stateFlows[0].value).isEqualTo(1)
        assertThat(stateFlows[1].value).isEqualTo(2)

        map.clear()

        assertThat(map.getValue("a")).isNull()
        assertThat(map.getValue("b")).isNull()
        // local references to stateFlows retain their values
        assertThat(stateFlows[0].value).isEqualTo(1)
        assertThat(stateFlows[1].value).isEqualTo(2)
    }

    @RepeatedTest(5)
    @Suppress("ExplicitGarbageCollectionCall")
    fun `unreferenced flow may be garbage collected`() {
        val map = SynchronizedWeakStateFlowMap<String, Int>()
        var onCreateCalls = 0

        var stateFlow: StateFlow<Int?>? =
            map.getOrCreateStateFlow("key", defaultValue = { 123 }, onCreate = { onCreateCalls++ })
        assertThat(onCreateCalls).isEqualTo(1)

        val flowReference = WeakReference(stateFlow)

        assertThat(flowReference.get()).isNotNull()

        System.gc()

        assertThat(flowReference.get()).isNotNull() // reference is maintained due to stateFlow var
        assertThat(map.getValue("key")).isEqualTo(123) // map can still access the value

        // a new flow is not created for the same key
        assertThat(map.getOrCreateStateFlow("key", defaultValue = { 0 }, onCreate = { onCreateCalls++ }))
            .isSameInstanceAs(stateFlow)
        assertThat(onCreateCalls).isEqualTo(1)

        @Suppress("UNUSED_VALUE")
        stateFlow = null
        System.gc()
        if (flowReference.get() != null) {
            // if GC did not collect the flow, try again after a delay
            Thread.sleep(10)
            System.gc()
        }

        assertThat(flowReference.get()).isNull() // reference has now been garbage collected
        assertThat(map.getValue("key")).isNull() // map no longer returns the value

        // a new flow is now created for the same key (keep a reference to it to avoid it being GC'd)
        @Suppress("UNUSED_VALUE")
        stateFlow = map.getOrCreateStateFlow("key", defaultValue = { 456 }, onCreate = { onCreateCalls++ })
        assertThat(map.getValue("key")).isEqualTo(456)
        assertThat(onCreateCalls).isEqualTo(2)
    }
}
