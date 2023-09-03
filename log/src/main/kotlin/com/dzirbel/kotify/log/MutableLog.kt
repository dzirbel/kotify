package com.dzirbel.kotify.log

import com.dzirbel.kotify.util.CurrentTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
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

/**
 * Creates a new [MutableLog] with the given [name] and [scope].
 *
 * @param T type of data associated with each [Log.Event]
 * @param name user-readable name used to identify the log in the debug UI and log files
 * @param scope [CoroutineScope] used to emit events
 * @param bufferCapacity number of events to buffer in the [Log.eventsFlow], may be increased for logs which are
 *  "bursty" and likely to emit many events in a short period of time
 */
@Suppress("FunctionNaming")
fun <T> MutableLog(name: String, scope: CoroutineScope, bufferCapacity: Int = 16): MutableLog<T> {
    return MutableLogImpl(name = name, scope = scope, bufferCapacity = bufferCapacity)
}

private class MutableLogImpl<T>(override val name: String, private val scope: CoroutineScope, bufferCapacity: Int) :
    MutableLog<T> {

    private val _events = mutableListOf<Log.Event<T>>()

    private val _eventsFlow = MutableSharedFlow<Log.Event<T>>(extraBufferCapacity = bufferCapacity)

    override val writeLock = Mutex()

    override val events: List<Log.Event<T>>
        get() = _events // do not create a copy of the list for performance, assumes callers will not mutate

    override val eventsFlow: SharedFlow<Log.Event<T>>
        get() = _eventsFlow.asSharedFlow()

    override fun log(event: Log.Event<T>): Log.Event<T> {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            writeLock.withLock {
                _events.add(event)
                _eventsFlow.emit(event)
            }
        }

        return event
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
