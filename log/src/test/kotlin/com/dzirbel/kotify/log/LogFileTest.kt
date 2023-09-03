package com.dzirbel.kotify.log

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.dzirbel.kotify.util.CurrentTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.time.Duration.Companion.microseconds

private class TestLog(override val name: String = "TestLog") : Log<Any?> {
    override val writeLock: Mutex
        get() = error("not implemented")
    override val events: List<Log.Event<Any?>>
        get() = error("not implemented")
    override val eventsFlow: Flow<Log.Event<Any?>>
        get() = error("not implemented")
}

class LogFileTest {
    data class FormatEventData(val log: Log<Any?> = TestLog(), val event: Log.Event<Any?>, val expected: String)

    @ParameterizedTest
    @MethodSource
    fun formatEvent(data: FormatEventData) {
        CurrentTime.mocked {
            assertThat(LogFile.formatEvent(data.log, data.event)).isEqualTo(data.expected)
        }
    }

    companion object {
        @JvmStatic
        fun formatEvent(): List<FormatEventData> {
            return listOf(
                FormatEventData(
                    event = Log.Event(title = "title", data = null, time = 0),
                    expected = "[TestLog] 1969-12-31 19:00:00.000 INFO    title",
                ),
                FormatEventData(
                    event = Log.Event(title = "title", data = Unit, time = 0),
                    expected = "[TestLog] 1969-12-31 19:00:00.000 INFO    title",
                ),
                FormatEventData(
                    event = Log.Event(title = "title", data = "data", time = 0),
                    expected = "[TestLog] 1969-12-31 19:00:00.000 INFO    title | data",
                ),
                FormatEventData(
                    event = Log.Event(title = "title", data = "data1\ndata2\ndata3", time = 0),
                    expected = """
                        [TestLog] 1969-12-31 19:00:00.000 INFO    title
                            data1
                            data2
                            data3
                    """.trimIndent(),
                ),
                FormatEventData(
                    event = Log.Event(title = "title", data = null, time = 0, type = Log.Event.Type.SUCCESS),
                    expected = "[TestLog] 1969-12-31 19:00:00.000 SUCCESS title",
                ),
                FormatEventData(
                    event = Log.Event(title = "title", data = null, time = 0, type = Log.Event.Type.WARNING),
                    expected = "[TestLog] 1969-12-31 19:00:00.000 WARNING title",
                ),
                FormatEventData(
                    event = Log.Event(title = "title", data = null, time = 0, type = Log.Event.Type.ERROR),
                    expected = "[TestLog] 1969-12-31 19:00:00.000 ERROR   title",
                ),
                FormatEventData(
                    event = Log.Event(title = "title", data = null, time = 1_681_977_723_000L),
                    expected = "[TestLog] 2023-04-20 04:02:03.000 INFO    title",
                ),
                FormatEventData(
                    event = Log.Event(title = "title", data = null, content = "content", time = 0),
                    expected = "[TestLog] 1969-12-31 19:00:00.000 INFO    title | content",
                ),
                FormatEventData(
                    event = Log.Event(title = "title", data = null, content = "content1\ncontent2\ncontent3", time = 0),
                    expected = """
                        [TestLog] 1969-12-31 19:00:00.000 INFO    title
                            content1
                            content2
                            content3
                    """.trimIndent(),
                ),
                FormatEventData(
                    event = Log.Event(title = "title", data = null, time = 0, duration = 12_345.microseconds),
                    expected = "[TestLog] 1969-12-31 19:00:00.000 INFO    title (12.345ms)",
                ),
                FormatEventData(
                    event = Log.Event(title = "title", data = null, time = 0, duration = 12_345_670.microseconds),
                    expected = "[TestLog] 1969-12-31 19:00:00.000 INFO    title (12s 345.670ms)",
                ),
            )
        }
    }
}
