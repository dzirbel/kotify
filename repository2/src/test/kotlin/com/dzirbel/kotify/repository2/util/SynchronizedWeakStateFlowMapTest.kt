package com.dzirbel.kotify.repository2.util

import assertk.assertThat
import assertk.assertions.hasSameSizeAs
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThan
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isSameAs
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

class SynchronizedWeakStateFlowMapTest {
    @Test
    fun `create new flow with null default`() {
        val map = SynchronizedWeakStateFlowMap<String, Int>()
        var onCreateCalls = 0

        val stateFlow = map.getOrCreateStateFlow("key", defaultValue = null, onCreate = { onCreateCalls++ })

        assertThat(stateFlow.value).isNull()
        assertThat(onCreateCalls).isEqualTo(1)
        assertThat(map.getValue("key")).isNull()
    }

    @Test
    fun `create new flow with non-null default`() {
        val default = 123
        val map = SynchronizedWeakStateFlowMap<String, Int>()
        var onCreateCalls = 0

        val stateFlow = map.getOrCreateStateFlow("key", defaultValue = default, onCreate = { onCreateCalls++ })

        assertThat(stateFlow.value).isEqualTo(default)
        assertThat(onCreateCalls).isEqualTo(1)
        assertThat(map.getValue("key")).isEqualTo(default)
    }

    @Test
    fun `subsequent calls to create new flow return existing instance`() {
        val map = SynchronizedWeakStateFlowMap<String, Int>()
        var onCreateCalls = 0

        val stateFlow = map.getOrCreateStateFlow("key", defaultValue = null, onCreate = { onCreateCalls++ })

        assertThat(stateFlow.value).isNull()
        assertThat(onCreateCalls).isEqualTo(1)
        assertThat(map.getValue("key")).isNull()

        val stateFlow2 = map.getOrCreateStateFlow("key", defaultValue = 123, onCreate = { onCreateCalls++ })

        assertThat(stateFlow2).isSameAs(stateFlow)
        assertThat(onCreateCalls).isEqualTo(1)
        assertThat(map.getValue("key")).isNull() // new default value is not used
    }

    @Test
    fun `creating flows in parallel only results in a single creation`() {
        val concurrency = 100 // number of parallel calls to make
        val map = SynchronizedWeakStateFlowMap<String, Int>()
        var onCreateCalls = 0
        val stateFlows = mutableSetOf<StateFlow<Int?>>()
        var sequentialLaunches = 0 // fuzzy count of launches which were sequential

        runBlocking(Dispatchers.IO) {
            val jobs = List(concurrency) { i ->
                launch {
                    val launchesInit = sequentialLaunches
                    val stateFlow = map.getOrCreateStateFlow("key", defaultValue = i, onCreate = { onCreateCalls++ })

                    delay(1) // delay to ensure calls are made concurrently

                    synchronized(stateFlows) { stateFlows.add(stateFlow) }
                    sequentialLaunches = launchesInit + 1
                }
            }

            jobs.joinAll()
        }

        assertThat(stateFlows).hasSize(1) // all StateFlows should be the same instance
        assertThat(onCreateCalls).isEqualTo(1)
        assertThat(map.getValue("key")).isEqualTo(stateFlows.first().value)
        assertThat(sequentialLaunches).isLessThan(concurrency) // verify that launches did not happen concurrently
    }

    @Test
    fun `batch create creates only missing flows`() {
        val map = SynchronizedWeakStateFlowMap<String, Int>()

        val keys1 = listOf("a", "b", "c")
        val createdKeys1 = mutableSetOf<String>()

        val flows1 = map.getOrCreateStateFlows(keys1, onCreate = { createdKeys1.addAll(it) })

        assertThat(flows1).hasSameSizeAs(keys1)
        assertThat(createdKeys1).containsExactlyElementsOfInAnyOrder(keys1)

        val keys2 = listOf("b", "d", "e", "c", "e")
        val createdKeys2 = mutableSetOf<String>()

        val flows2 = map.getOrCreateStateFlows(keys2, onCreate = { createdKeys2.addAll(it) })

        assertThat(flows2).hasSameSizeAs(keys2)
        assertThat(createdKeys2).containsExactlyElementsOfInAnyOrder(keys2.minus(keys1).toSet())
        assertThat(flows2).index(0).isSameAs(flows1[1]) // same flows for "b"
        assertThat(flows2).index(3).isSameAs(flows1[2]) // same flows for "c"
        assertThat(flows2).index(2).isSameAs(flows2[4]) // same flows for first and second "e"
    }

    @Test
    fun `batch creating flows in parallel only results in a single creation`() {
        val concurrency = 100 // number of parallel calls to make
        val keys = listOf("a", "b", "c")
        val map = SynchronizedWeakStateFlowMap<String, Int>()
        var onCreateCalls = 0
        val stateFlows = mutableSetOf<StateFlow<Int?>>()
        var sequentialLaunches = 0 // fuzzy count of launches which were sequential

        runBlocking(Dispatchers.IO) {
            val jobs = List(concurrency) {
                launch {
                    val launchesInit = sequentialLaunches
                    val createdStateFlows = map.getOrCreateStateFlows(keys, onCreate = { onCreateCalls += it.size })

                    delay(1) // delay to ensure calls are made concurrently

                    synchronized(stateFlows) { stateFlows.addAll(createdStateFlows) }
                    sequentialLaunches = launchesInit + 1
                }
            }

            jobs.joinAll()
        }

        assertThat(stateFlows).hasSameSizeAs(keys) // all StateFlows should be the same instance per key
        assertThat(onCreateCalls).isEqualTo(keys.size)
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

        val stateFlow = map.getOrCreateStateFlow("key", defaultValue = 456)

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

        val stateFlow = map.getOrCreateStateFlow("key", defaultValue = 0)

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
            map.getOrCreateStateFlow("key", defaultValue = 123, onCreate = { onCreateCalls++ })
        assertThat(onCreateCalls).isEqualTo(1)

        val flowReference = WeakReference(stateFlow)

        assertThat(flowReference.get()).isNotNull()

        System.gc()

        assertThat(flowReference.get()).isNotNull() // reference is maintained due to stateFlow var
        assertThat(map.getValue("key")).isEqualTo(123) // map can still access the value

        // a new flow is not created for the same key
        assertThat(map.getOrCreateStateFlow("key", defaultValue = 0, onCreate = { onCreateCalls++ }))
            .isSameAs(stateFlow)
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
        stateFlow = map.getOrCreateStateFlow("key", defaultValue = 456, onCreate = { onCreateCalls++ })
        assertThat(map.getValue("key")).isEqualTo(456)
        assertThat(onCreateCalls).isEqualTo(2)
    }
}
