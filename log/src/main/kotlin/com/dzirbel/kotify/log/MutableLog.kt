package com.dzirbel.kotify.log

import com.dzirbel.kotify.util.CurrentTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration

/**
 * A [Log] which allows logging new events.
 *
 * TODO add logging to disk
 */
interface MutableLog<T> : Log<T> {
    /**
     * Logs a new [event].
     */
    fun log(event: Log.Event<T>): Log.Event<T>
}

@Suppress("FunctionNaming")
fun <T> MutableLog(name: String, scope: CoroutineScope): MutableLog<T> = MutableLogImpl(name, scope)

private class MutableLogImpl<T>(override val name: String, private val scope: CoroutineScope) : MutableLog<T> {
    private val _events = mutableListOf<Log.Event<T>>()

    private val _eventsFlow = MutableSharedFlow<Log.Event<T>>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val writeLock = Mutex()

    override val events: List<Log.Event<T>>
        get() = _events // do not create a copy of the list for performance, assumes callers will not mutate

    override val eventsFlow: SharedFlow<Log.Event<T>>
        get() = _eventsFlow.asSharedFlow()

    override fun log(event: Log.Event<T>): Log.Event<T> {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            writeLock.withLock {
                _events.add(event)
                _eventsFlow.forceEmit(event)
            }
        }

        return event
    }

    private fun <T> MutableSharedFlow<T>.forceEmit(value: T) {
        check(tryEmit(value)) { "failed to emit $value" }
    }
}

fun <T> MutableLog<T>.info(
    title: String,
    data: T,
    content: String? = null,
    time: Long = CurrentTime.millis,
    duration: Duration? = null,
): Log.Event<T> {
    return log(
        Log.Event(
            title = title,
            data = data,
            content = content,
            time = time,
            duration = duration,
            type = Log.Event.Type.INFO,
        ),
    )
}

fun MutableLog<Unit>.info(
    title: String,
    content: String? = null,
    time: Long = CurrentTime.millis,
    duration: Duration? = null,
): Log.Event<Unit> {
    return info(title = title, data = Unit, content = content, time = time, duration = duration)
}

fun <T> MutableLog<T>.success(
    title: String,
    data: T,
    content: String? = null,
    time: Long = CurrentTime.millis,
    duration: Duration? = null,
): Log.Event<T> {
    return log(
        Log.Event(
            title = title,
            data = data,
            content = content,
            time = time,
            duration = duration,
            type = Log.Event.Type.SUCCESS,
        ),
    )
}

fun MutableLog<Unit>.success(
    title: String,
    content: String? = null,
    time: Long = CurrentTime.millis,
    duration: Duration? = null,
): Log.Event<Unit> {
    return success(title = title, data = Unit, content = content, time = time, duration = duration)
}

fun <T> MutableLog<T>.warn(
    title: String,
    data: T,
    content: String? = null,
    time: Long = CurrentTime.millis,
    duration: Duration? = null,
): Log.Event<T> {
    return log(
        Log.Event(
            title = title,
            data = data,
            content = content,
            time = time,
            duration = duration,
            type = Log.Event.Type.WARNING,
        ),
    )
}

fun MutableLog<Unit>.warn(
    title: String,
    content: String? = null,
    time: Long = CurrentTime.millis,
    duration: Duration? = null,
): Log.Event<Unit> {
    return warn(title = title, data = Unit, content = content, time = time, duration = duration)
}

fun <T> MutableLog<T>.warn(
    throwable: Throwable,
    data: T,
    title: String = throwable.message ?: throwable.cause?.message ?: "Unknown error",
    content: String? = throwable.stackTraceToString(),
    time: Long = CurrentTime.millis,
    duration: Duration? = null,
): Log.Event<T> {
    return warn(title = title, data = data, content = content, time = time, duration = duration)
}

fun MutableLog<Unit>.warn(
    throwable: Throwable,
    title: String = throwable.message ?: throwable.cause?.message ?: "Unknown error",
    content: String? = throwable.stackTraceToString(),
    time: Long = CurrentTime.millis,
    duration: Duration? = null,
): Log.Event<Unit> {
    return warn(title = title, content = content, time = time, duration = duration)
}

fun <T> MutableLog<T>.error(
    title: String,
    data: T,
    content: String? = null,
    time: Long = CurrentTime.millis,
    duration: Duration? = null,
): Log.Event<T> {
    return log(
        Log.Event(
            title = title,
            data = data,
            content = content,
            time = time,
            duration = duration,
            type = Log.Event.Type.ERROR,
        ),
    )
}

fun MutableLog<Unit>.error(
    title: String,
    content: String? = null,
    time: Long = CurrentTime.millis,
    duration: Duration? = null,
): Log.Event<Unit> {
    return error(title = title, data = Unit, content = content, time = time, duration = duration)
}

fun <T> MutableLog<T>.error(
    throwable: Throwable,
    data: T,
    title: String = throwable.message ?: throwable.cause?.message ?: "Unknown error",
    content: String? = throwable.stackTraceToString(),
    time: Long = CurrentTime.millis,
    duration: Duration? = null,
): Log.Event<T> {
    return error(title = title, data = data, content = content, time = time, duration = duration)
}

fun MutableLog<Unit>.error(
    throwable: Throwable,
    title: String = throwable.message ?: throwable.cause?.message ?: "Unknown error",
    content: String? = throwable.stackTraceToString(),
    time: Long = CurrentTime.millis,
    duration: Duration? = null,
): Log.Event<Unit> {
    return error(title = title, content = content, time = time, duration = duration)
}
