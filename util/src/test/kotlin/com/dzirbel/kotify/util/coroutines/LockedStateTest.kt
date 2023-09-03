package com.dzirbel.kotify.util.coroutines

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Test

class LockedStateTest {
    @Test
    fun `initial and emitted values are reflected`() {
        val mutex = Mutex()
        val base = MutableSharedFlow<String>()
        runTest {
            base.emit("a")

            val state = mutex.lockedState(
                initial = "i1",
                initializeWithLock = { "i2" },
                scope = this,
                flow = { initial ->
                    base.runningFold(initial) { acc, value -> "$acc $value" }
                },
            )

            assertThat(state.value).isEqualTo("i2")
            runCurrent()
            assertThat(state.value).isEqualTo("i2")

            base.emit("b")
            assertThat(state.value).isEqualTo("i2 b")
            runCurrent()
            assertThat(state.value).isEqualTo("i2 b")

            base.emit("c")
            assertThat(state.value).isEqualTo("i2 b c")
            runCurrent()
            assertThat(state.value).isEqualTo("i2 b c")

            coroutineContext.cancelChildren() // cancel to stop flow collection
        }
    }

    @Test
    fun `replayed values are reflected`() {
        val mutex = Mutex()
        val base = MutableSharedFlow<String>(replay = 3)
        runTest {
            base.emit("a")
            base.emit("b")
            base.emit("c")
            base.emit("d")

            val state = mutex.lockedState(
                initial = "i1",
                initializeWithLock = { "i2" },
                scope = this,
                flow = { initial ->
                    base.runningFold(initial) { acc, value -> "$acc $value" }
                },
            )

            assertThat(state.value).isEqualTo("i2 b c d")
            runCurrent()
            assertThat(state.value).isEqualTo("i2 b c d")

            base.emit("e")
            // e is not present immediately, apparently because the SharedFlow does not eagerly replace values from the
            // replay cache until they are collected downstream
            assertThat(state.value).isEqualTo("i2 b c d")
            runCurrent()
            assertThat(state.value).isEqualTo("i2 b c d e")

            coroutineContext.cancelChildren() // cancel to stop flow collection
        }
    }

    @Test
    fun `initial value is reflected when yielding initializeWithLock`() {
        val mutex = Mutex()
        val base = MutableSharedFlow<String>()
        runTest {
            val state = mutex.lockedState(
                initial = "i1",
                initializeWithLock = {
                    yield()
                    "i2"
                },
                scope = this,
                flow = { initial ->
                    base.runningFold(initial) { acc, value -> "$acc $value" }
                },
            )

            assertThat(state.value).isEqualTo("i1")

            runCurrent()

            assertThat(state.value).isEqualTo("i2")

            base.emit("a")
            assertThat(state.value).isEqualTo("i2 a")
            runCurrent()
            assertThat(state.value).isEqualTo("i2 a")

            coroutineContext.cancelChildren() // cancel to stop flow collection
        }
    }

    @Test
    fun `initialize and collection wait until lock is available`() {
        val mutex = Mutex()
        val base = MutableSharedFlow<String>()
        runTest {
            mutex.lock()

            val state = mutex.lockedState(
                initial = "i1",
                initializeWithLock = {
                    assertThat(mutex.isLocked).isTrue()
                    "i2"
                },
                scope = this,
                flow = { initial ->
                    assertThat(mutex.isLocked).isTrue()
                    base.runningFold(initial) { acc, value -> "$acc $value" }
                },
            )

            assertThat(state.value).isEqualTo("i1")
            runCurrent()
            assertThat(state.value).isEqualTo("i1")

            base.emit("a")
            runCurrent()
            assertThat(state.value).isEqualTo("i1")

            mutex.unlock()
            assertThat(state.value).isEqualTo("i1")
            runCurrent()
            assertThat(state.value).isEqualTo("i2")
            assertThat(mutex.isLocked).isFalse() // lock is released again

            base.emit("b")
            assertThat(state.value).isEqualTo("i2 b")
            runCurrent()
            assertThat(state.value).isEqualTo("i2 b")

            coroutineContext.cancelChildren() // cancel to stop flow collection
        }
    }
}
