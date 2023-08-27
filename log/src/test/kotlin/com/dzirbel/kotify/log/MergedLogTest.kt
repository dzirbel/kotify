package com.dzirbel.kotify.log

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import com.dzirbel.kotify.util.CurrentTime
import com.dzirbel.kotify.util.collectingToList
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class MergedLogTest {
    @Test
    fun test() {
        CurrentTime.mocked {
            val log1 = MutableLog<Log.Event>()
            val log2 = MutableLog<Log.Event>()
            val log3 = MutableLog<Log.Event>()

            val e1 = log1.info("1A")

            val merged = listOf(log1, log2, log3).merged()
            assertThat(merged.events).containsExactly(e1)

            runTest {
                collectingToList(merged.eventsFlow) { list ->
                    assertThat(list).isEmpty()

                    val e2 = log2.info("2A")
                    runCurrent()

                    assertThat(merged.events).containsExactly(e1, e2)
                    assertThat(list).containsExactly(e2)

                    val e3 = log3.info("3A")
                    val e4 = log3.info("3B")
                    val e5 = log3.info("3C")
                    runCurrent()

                    assertThat(merged.events).containsExactly(e1, e2, e3, e4, e5)
                    assertThat(list).containsExactly(e2, e3, e4, e5)
                }
            }
        }
    }
}
