package com.dzirbel.kotify.log

import com.dzirbel.kotify.util.CurrentTime
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

/**
 * Represents a live log of events.
 *
 * This is a simple, standard interface that various aspects of the application can expose to allow displaying logs in
 * the UI and/or writing them to disk.
 */
interface Log<E : Log.Event> {

    /**
     * A single event that can be logged.
     *
     * Open to allow for custom events with additional fields.
     *
     * TODO should be @Stable?
     */
    open class Event(
        val title: String,
        val content: String? = null,
        val type: Type = Type.INFO,
        val time: Long = CurrentTime.millis,
    ) {
        enum class Type {
            INFO, SUCCESS, WARNING, ERROR
        }
    }

    /**
     * Returns a snapshot of the current events in this log.
     */
    val events: ImmutableList<E>

    /**
     * Returns a [Flow] of new events emitted by this log.
     */
    val eventsFlow: Flow<E>
}
