package com.dzirbel.kotify.log

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.sync.Mutex

class FakeLog<T>(override val name: String = "Log") : Log<T> {
    override val writeLock: Mutex = Mutex()
    override val events: List<Log.Event<T>> = emptyList()
    override val eventsFlow: Flow<Log.Event<T>> = emptyFlow()
}
