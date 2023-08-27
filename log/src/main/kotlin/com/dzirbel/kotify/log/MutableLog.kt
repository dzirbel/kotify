package com.dzirbel.kotify.log

import com.dzirbel.kotify.util.CurrentTime
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * A [Log] which allows [log]ging new events and [clear]ing the existing events.
 */
interface MutableLog<E : Log.Event> : Log<E> {
    /**
     * Logs a new [event].
     */
    fun log(event: E): E

    /**
     * Clears all existing events.
     */
    fun clear()
}

@Suppress("FunctionNaming")
fun <E : Log.Event> MutableLog(): MutableLog<E> = MutableLogImpl()

private class MutableLogImpl<E : Log.Event> : MutableLog<E> {
    private var _events = persistentListOf<E>()

    private val _eventsFlow = MutableSharedFlow<E>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val events: ImmutableList<E>
        get() = _events

    override val eventsFlow: SharedFlow<E>
        get() = _eventsFlow.asSharedFlow()

    override fun log(event: E): E {
        synchronized(this) {
            _events = _events.add(event)
            _eventsFlow.forceEmit(event)
        }
        return event
    }

    override fun clear() {
        _events = persistentListOf()
    }

    private fun <T> MutableSharedFlow<T>.forceEmit(value: T) {
        check(tryEmit(value)) { "failed to emit $value" }
    }
}

fun MutableLog<Log.Event>.info(title: String, content: String? = null, time: Long = CurrentTime.millis): Log.Event {
    return log(event = Log.Event(title = title, content = content, type = Log.Event.Type.INFO, time = time))
}

fun MutableLog<Log.Event>.success(title: String, content: String? = null, time: Long = CurrentTime.millis): Log.Event {
    return log(event = Log.Event(title = title, content = content, type = Log.Event.Type.SUCCESS, time = time))
}

fun MutableLog<Log.Event>.warn(title: String, content: String? = null, time: Long = CurrentTime.millis): Log.Event {
    return log(event = Log.Event(title = title, content = content, type = Log.Event.Type.WARNING, time = time))
}

fun MutableLog<Log.Event>.error(title: String, content: String? = null, time: Long = CurrentTime.millis): Log.Event {
    return log(event = Log.Event(title = title, content = content, type = Log.Event.Type.ERROR, time = time))
}

fun MutableLog<Log.Event>.error(
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
