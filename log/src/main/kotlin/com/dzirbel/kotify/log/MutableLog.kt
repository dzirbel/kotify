package com.dzirbel.kotify.log

import com.dzirbel.kotify.util.CurrentTime
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A [Log] which allows logging new events.
 *
 * TODO add logging to disk
 */
interface MutableLog<E : Log.Event> : Log<E> {
    /**
     * Logs a new [event].
     */
    suspend fun log(event: E): E
}

@Suppress("FunctionNaming")
fun <E : Log.Event> MutableLog(name: String): MutableLog<E> = MutableLogImpl(name)

private class MutableLogImpl<E : Log.Event>(override val name: String) : MutableLog<E> {
    private val _events = mutableListOf<E>()

    private val _eventsFlow = MutableSharedFlow<E>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val writeLock = Mutex()

    override val events: List<E>
        get() = _events // do not create a copy of the list for performance, assumes callers will not mutate

    override val eventsFlow: SharedFlow<E>
        get() = _eventsFlow.asSharedFlow()

    override suspend fun log(event: E): E {
        writeLock.withLock {
            _events.add(event)
            _eventsFlow.forceEmit(event)
        }

        return event
    }

    private fun <T> MutableSharedFlow<T>.forceEmit(value: T) {
        check(tryEmit(value)) { "failed to emit $value" }
    }
}

suspend fun MutableLog<Log.Event>.info(
    title: String,
    content: String? = null,
    time: Long = CurrentTime.millis,
): Log.Event {
    return log(event = Log.Event(title = title, content = content, type = Log.Event.Type.INFO, time = time))
}

suspend fun MutableLog<Log.Event>.success(
    title: String,
    content: String? = null,
    time: Long = CurrentTime.millis,
): Log.Event {
    return log(event = Log.Event(title = title, content = content, type = Log.Event.Type.SUCCESS, time = time))
}

suspend fun MutableLog<Log.Event>.warn(
    title: String,
    content: String? = null,
    time: Long = CurrentTime.millis,
): Log.Event {
    return log(event = Log.Event(title = title, content = content, type = Log.Event.Type.WARNING, time = time))
}

suspend fun MutableLog<Log.Event>.warn(
    throwable: Throwable,
    title: String? = null,
    content: String? = null,
    time: Long = CurrentTime.millis,
): Log.Event {
    return log(
        event = Log.Event(
            title = title ?: throwable.message ?: "Unknown error",
            content = content ?: throwable.stackTraceToString(),
            type = Log.Event.Type.WARNING,
            time = time,
        ),
    )
}

suspend fun MutableLog<Log.Event>.error(
    title: String,
    content: String? = null,
    time: Long = CurrentTime.millis,
): Log.Event {
    return log(event = Log.Event(title = title, content = content, type = Log.Event.Type.ERROR, time = time))
}

suspend fun MutableLog<Log.Event>.error(
    throwable: Throwable,
    title: String? = null,
    content: String? = null,
    time: Long = CurrentTime.millis,
): Log.Event {
    return log(
        event = Log.Event(
            title = title ?: throwable.message ?: "Unknown error",
            content = content ?: throwable.stackTraceToString(),
            type = Log.Event.Type.ERROR,
            time = time,
        ),
    )
}
