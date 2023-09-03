package com.dzirbel.kotify

import androidx.compose.runtime.Stable
import com.dzirbel.kotify.Logger.Event
import com.dzirbel.kotify.util.CurrentTime
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * A simple in-memory log of [Event]s storing arbitrary data of type [T], which can be [log]ed variously throughout the
 * application and retrieved to be exposed in the UI by [eventsFlow].
 *
 * TODO rework logging
 */
@Stable
sealed class Logger<T> {
    /**
     * A single event that can be logged.
     */
    data class Event<T>(
        val title: String,
        val content: String? = null,
        val data: T,
        val type: Type = Type.INFO,
        val time: Long = CurrentTime.millis,
    ) {
        enum class Type {
            INFO, SUCCESS, WARNING, ERROR
        }
    }

    private val events = mutableListOf<Event<T>>()
    private val mutableEventsFlow = MutableSharedFlow<List<Event<T>>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /**
     * A [SharedFlow] view of the events held by this logged. Replays the current list of events to new subscribers.
     */
    val eventsFlow: SharedFlow<List<Event<T>>>
        get() = mutableEventsFlow.asSharedFlow()

    protected fun log(
        title: String,
        content: String? = null,
        data: T,
        type: Event.Type = Event.Type.INFO,
        time: Long = CurrentTime.millis,
    ) {
        val event = Event(title = title, content = content, data = data, type = type, time = time)
        synchronized(events) {
            events.add(event)
            mutableEventsFlow.tryEmit(ArrayList(events))
        }
    }

    fun clear() {
        synchronized(events) {
            events.clear()
        }

        mutableEventsFlow.tryEmit(emptyList())
    }

    object UI : Logger<Unit>()
}
