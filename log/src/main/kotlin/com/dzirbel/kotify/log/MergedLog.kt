package com.dzirbel.kotify.log

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge

/**
 * Returns a [Log] which merges the events from all the [Log]s in this [Iterable].
 */
fun <E : Log.Event> Iterable<Log<E>>.merged(): Log<E> = MergedLog(logs = this)

private class MergedLog<E : Log.Event>(private val logs: Iterable<Log<E>>) : Log<E> {
    override val events: ImmutableList<E>
        get() = logs.flatMap { it.events }.toPersistentList()

    override val eventsFlow: Flow<E>
        get() = logs.map { it.eventsFlow }.merge()
}
