package com.dominiczirbel.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.dominiczirbel.ui.util.RemoteState
import com.dominiczirbel.ui.util.collectAsStateSwitchable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlin.coroutines.CoroutineContext

abstract class Presenter<State, Event, Result>(
    private val key: Any? = null,
    private val ignoreInitialState: Boolean = true,
    private val startingEvents: List<Event> = emptyList()
) {
    abstract val initialState: State

    val events = MutableSharedFlow<Event>()

    @Composable
    fun state(context: CoroutineContext = Dispatchers.IO): RemoteState<State> {
        return remember(key) {
            events
                .onStart {
                    startingEvents.forEach { emit(it) }
                }
                .let { reactTo(it) }
                .scan(initialState) { state, result ->
                    apply(state, result)
                }
                .let { if (ignoreInitialState) it.drop(1) else it }
                .map<State, RemoteState<State>> { RemoteState.Success(it) }
                .catch {
                    it.printStackTrace()
                    emit(RemoteState.Error(it))
                }
        }
            .collectAsStateSwitchable(initial = { RemoteState.Loading() }, context = context, key = key)
            .value
    }

    open fun reactTo(events: Flow<Event>): Flow<Result> {
        return events.flatMapMerge(transform = ::reactTo)
    }

    abstract fun reactTo(event: Event): Flow<Result>
    abstract fun apply(state: State, result: Result): State
}
