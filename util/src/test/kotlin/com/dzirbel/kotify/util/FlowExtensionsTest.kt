package com.dzirbel.kotify.util

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class FlowExtensionsTest {
    @Test
    fun ignoreEmitsNothing() {
        val flow = flowOf(1, 2, 3)

        runTest {
            assertThat(flow.ignore<Unit>().toList()).isEmpty()
        }
    }

    @Test
    fun ignoreCollectsSourceFlow() {
        val values = mutableListOf<Int>()
        val flow = flowOf(1, 2, 3)
            .onEach { values.add(it) }

        runTest {
            assertThat(values).isEmpty()
            assertThat(flow.ignore<Unit>().toList()).isEmpty()
            assertThat(values).containsExactly(1, 2, 3)
        }
    }
}
