package com.dzirbel.kotify.log

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class FlowViewTest {
    @Test
    fun `initial list is filtered and sorted`() {
        val flow = MutableSharedFlow<String>()
        val initial = listOf("a", "b", "aab", "acde", "ccc", "a")

        val view = FlowView<String>(
            filter = { !it.contains("b") },
            sort = Comparator.comparing { it.length },
        )

        runTest {
            val viewState = view.viewState(flow = flow, initial = initial, scope = this)

            assertThat(viewState.value).containsExactly("a", "a", "ccc", "acde")

            coroutineContext.cancelChildren()
        }
    }

    @Test
    fun `filtered values are not emitted`() {
        val flow = MutableSharedFlow<String>()

        val view = FlowView<String>(filter = { !it.contains("b") })

        runTest {
            val viewState = view.viewState(flow = flow, initial = emptyList(), scope = this)

            assertThat(viewState.value).isEmpty()
            flow.emit("a")
            assertThat(viewState.value).containsExactly("a")
            flow.emit("ab")
            assertThat(viewState.value).containsExactly("a")
            flow.emit("cde")
            assertThat(viewState.value).containsExactly("a", "cde")

            coroutineContext.cancelChildren()
        }
    }

    @Test
    fun `values are inserted based on sort`() {
        val flow = MutableSharedFlow<String>()

        val view = FlowView<String>(sort = Comparator.comparing { it.length })

        runTest {
            val viewState = view.viewState(flow = flow, initial = emptyList(), scope = this)

            assertThat(viewState.value).isEmpty()
            flow.emit("abc")
            assertThat(viewState.value).containsExactly("abc")
            flow.emit("a")
            assertThat(viewState.value).containsExactly("a", "abc")
            flow.emit("a")
            assertThat(viewState.value).containsExactly("a", "a", "abc")
            flow.emit("abcd")
            assertThat(viewState.value).containsExactly("a", "a", "abc", "abcd")
            flow.emit("dd")
            assertThat(viewState.value).containsExactly("a", "a", "dd", "abc", "abcd")

            coroutineContext.cancelChildren()
        }
    }

    @Test
    fun `values emitted between initial read and collection start are not missed`() {
        val flow = MutableSharedFlow<String>()

        val view = FlowView<String>()

        runTest {
            launch { flow.emit("a") }

            val viewState = view.viewState(flow = flow, initial = emptyList(), scope = this)

            launch { flow.emit("b") }

            assertThat(viewState.value).isEmpty()

            runCurrent()

            assertThat(viewState.value).containsExactly("a", "b")

            coroutineContext.cancelChildren()
        }
    }
}
