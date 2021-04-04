package com.dominiczirbel.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.dominiczirbel.ui.util.RemoteState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

abstract class Presenter<State, Event>(
    private val key: Any? = null,
    private val eventMergeStrategy: EventMergeStrategy = EventMergeStrategy.MERGE,
    private val startingEvents: List<Event>? = null,
    initialState: State
) {
    enum class EventMergeStrategy { LATEST, MERGE }

    private val stateFlow = MutableStateFlow(initialState)

    private lateinit var scope: CoroutineScope

    val events = MutableSharedFlow<Event>()

    // generally only used for testing; Composable usages should use state()
    val state: State
        get() = stateFlow.value

    fun emitEvent(event: Event) {
        scope.launch { events.emit(event) }
    }

    suspend fun open(startingEvents: List<Event>? = this.startingEvents) {
        reactTo(
            if (startingEvents == null) {
                events
            } else {
                events.onStart { startingEvents.forEach { emit(it) } }
            }
                .onEach { println("[${this::class.simpleName}] Event -> $it") }
        )
    }

    @Composable
    fun state(context: CoroutineContext = Dispatchers.IO, startingEvents: List<Event>? = this.startingEvents): State {
        scope = rememberCoroutineScope { context }
        remember(key) {
            scope.launch {
                open(startingEvents = startingEvents)
            }
        }

        return stateFlow.collectAsState(context = context).value
    }

    protected fun mutateState(transform: (State) -> State?) {
        synchronized(this) {
            transform(state)?.let {
                stateFlow.value = it
                    .also { println("[${this::class.simpleName}] State -> $it") }
            }
        }
    }

    open suspend fun reactTo(events: Flow<Event>) {
        return when (eventMergeStrategy) {
            EventMergeStrategy.LATEST -> events.collectLatest { reactTo(it) }
            EventMergeStrategy.MERGE -> events.flatMapMerge { flow<Unit> { reactTo(it) } }.collect()
        }
    }

    abstract suspend fun reactTo(event: Event)

    @Suppress("unused") // false positive for receiver parameter: state parameter is different
    fun <T> Presenter<RemoteState<T>, *>.mutateRemoteState(default: T? = null, transform: (T) -> T?) {
        mutateState { remoteState ->
            ((remoteState as? RemoteState.Success)?.data ?: default)
                ?.let(transform)
                ?.let { RemoteState.Success(it) }
        }
    }
}
