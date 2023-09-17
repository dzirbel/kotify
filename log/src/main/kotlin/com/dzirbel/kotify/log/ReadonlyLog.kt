package com.dzirbel.kotify.log

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex

/**
 * Returns a read-only view of this [Log], suitable to be exposed from a component that keeps a private [MutableLog].
 */
fun <T> Log<T>.asLog(): Log<T> = ReadonlyLog(this)

/**
 * Returns a read-only view of this [Log], with each event's data mapped by [mapper].
 */
fun <T, R> Log<T>.asLog(mapper: (T) -> R): Log<R> = MappedReadonlyLog(this, mapper)

private class ReadonlyLog<T>(log: Log<T>) : Log<T> by log

private class MappedReadonlyLog<T, R>(private val log: Log<T>, private val mapper: (T) -> R) : Log<R> {
    override val name: String
        get() = log.name

    override val writeLock: Mutex
        get() = log.writeLock

    override val events: List<Log.Event<R>>
        get() = log.events.map { it.mapped() }

    override val eventsFlow: Flow<Log.Event<R>>
        get() = log.eventsFlow.map { it.mapped() }

    private fun Log.Event<T>.mapped(): Log.Event<R> {
        return Log.Event(
            title = title,
            data = mapper(data),
            content = content,
            type = type,
            time = time,
            duration = duration,
        )
    }
}
