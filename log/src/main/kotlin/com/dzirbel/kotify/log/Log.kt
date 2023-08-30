package com.dzirbel.kotify.log

import com.dzirbel.kotify.util.CurrentTime
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

/**
 * Represents a live log of events.
 *
 * This is a simple, standard interface that various aspects of the application can expose to allow displaying logs in
 * the UI and/or writing them to disk.
 */
interface Log<T> {

    /**
     * A single event that can be logged, with custom data of type [T].
     *
     * TODO should be @Stable?
     */
    data class Event<T>(
        val title: String,
        val data: T,
        val content: String? = null,
        val type: Type = Type.INFO,
        val time: Long = CurrentTime.millis,
        val duration: Duration? = null,
    ) {
        enum class Type {
            INFO, SUCCESS, WARNING, ERROR
        }
    }

    /**
     * The user-readable name of this log.
     */
    val name: String

    /**
     * Returns a snapshot of the current events in this log.
     */
    val events: List<Event<T>>

    /**
     * Returns a [Flow] of new events emitted by this log.
     */
    val eventsFlow: Flow<Event<T>>
}
