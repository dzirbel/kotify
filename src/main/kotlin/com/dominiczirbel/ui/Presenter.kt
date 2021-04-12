package com.dominiczirbel.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dominiczirbel.ui.Presenter.StateOrError.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import java.io.Closeable
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * A presenter abstraction which controls the state of a particular piece of the UI.
 *
 * The presenter continually listens for [events] and processes them via [reactTo], which calls [mutateState] to update
 * the view [state].
 */
abstract class Presenter<State, Event> constructor(
    /**
     * The [CoroutineScope] under which this presenter operates, typically bound to the UI's point in the composition
     * (i.e. from [androidx.compose.runtime.rememberCoroutineScope]).
     */
    protected val scope: CoroutineScope,

    /**
     * The initial [State] of the content.
     */
    initialState: State,

    /**
     * An optional key by which the event flow collection is remembered; the same event flow will be used as long as
     * this value stays the same but will be recreated when the key changes. This is necessary because calls to [state]
     * will be at the same point in the composition even if the presenter object changes, and so the flow may not be
     * reset by default.
     */
    private val key: Any? = null,

    /**
     * The strategy by which to handle concurrent events, i.e. events whose processing (via [reactTo]) has not completed
     * before the next event is emitted.
     */
    private val eventMergeStrategy: EventMergeStrategy = EventMergeStrategy.MERGE,

    /**
     * An optional list of events which should be emitted at the beginning of the event flow, e.g. to load content.
     */
    private val startingEvents: List<Event>? = null
) : Closeable {
    /**
     * Determines how concurrent events are merged, i.e. when a new event is emitted before the previous event was fully
     * processed.
     */
    enum class EventMergeStrategy {
        /**
         * Only the latest event is processed; processing of previous events is cancelled when a new event arrives.
         */
        LATEST,

        /**
         * All events are processed concurrently.
         */
        MERGE
    }

    /**
     * Represents a state of the view or an error.
     */
    sealed class StateOrError<State> {
        /**
         * Gets the last non-error [State].
         */
        abstract val safeState: State

        /**
         * Gets the current [State], throwing an exception if it is an error state.
         */
        abstract val stateOrThrow: State

        /**
         * Represents a successful view [state].
         */
        data class State<State>(val state: State) : StateOrError<State>() {
            override val safeState: State = state
            override val stateOrThrow: State = state
        }

        /**
         * Represents an error case, with both the last non-error successful state [lastState] and the [throwable] that
         * that caused the current error.
         */
        data class Error<State>(val lastState: State, val throwable: Throwable) : StateOrError<State>() {
            override val safeState: State = lastState

            override val stateOrThrow: State
                get() = throw throwable
        }
    }

    /**
     * Exposes the current state for tests, where the composable [state] cannot be called.
     */
    internal val testState: StateOrError<State>
        get() = synchronized(this) { stateFlow.value }

    /**
     * A [MutableStateFlow] which exposes the current state (via the [StateOrError] wrapper, possibly wrapping an
     * exception instead). Should only be modified internally, and writes must be synchronized.
     *
     * Ideally we might use a simpler mechanism to represent the state, but e.g. [androidx.compose.runtime.MutableState]
     * does not allow writes during a snapshot and so cannot support arbitrary concurrency.
     */
    private val stateFlow = MutableStateFlow<StateOrError<State>>(State(initialState))

    private val events = MutableSharedFlow<Event>()

    private val logTag by lazy { this::class.simpleName }

    /**
     * A list of accumulated errors from the event stream.
     */
    var errors by mutableStateOf(emptyList<Throwable>())

    /**
     * The core event [Flow], which includes emitting [startingEvents], reacting to events according to the
     * [eventMergeStrategy] with [reactTo], and catching errors.
     *
     * This must be its own function to allow restarting the event flow on error by making a recursive call to [flow].
     */
    private fun flow(startingEvents: List<Event>? = this.startingEvents): Flow<Event> {
        return events
            .onStart { startingEvents?.forEach { emit(it) } }
            .onEach { log("Event -> $it") }
            .let { flow -> reactTo(flow) }
            .catch {
                onError(it)
                emitAll(flow(null))
            }
    }

    /**
     * Opens this presenter, indefinitely waiting for events in the event flow and handling them.
     *
     * This function never returns.
     *
     * Typically only used from tests, most usages should call [state] instead, which opens the presenter and collects
     * its state as a composition-aware state.
     */
    internal suspend fun open() {
        flow().collect()
    }

    /**
     * Closes the presenter, cleaning up (i.e. cancelling) any background jobs on its [scope].
     *
     * Typically only used to cleanup from tests.
     */
    override fun close() {}

    /**
     * Listens and handles events for this presenter and returns its current state. This function is appropriate to be
     * called in a composition and returns a composition-aware state.
     */
    @Composable
    fun state(context: CoroutineContext = EmptyCoroutineContext): StateOrError<State> {
        remember(key) {
            scope.launch(context = context) {
                open()
            }
        }

        return stateFlow.collectAsState(context = context).value
    }

    /**
     * Emits the given [event], possibly suspending until there is enough buffer space in the event flow. Most cases
     * should use [emitAsync] instead.
     */
    suspend fun emit(event: Event) {
        events.emit(event)
    }

    /**
     * Emits the given [events] on a new coroutine spawned from this presenter's [scope], and returns immediately.
     */
    fun emitAsync(vararg events: Event, context: CoroutineContext = EmptyCoroutineContext) {
        println("emitting ${events.contentToString()}")
        scope.launch(context = context) { events.forEach { emit(it) } }
    }

    /**
     * Mutates the current [state] of the view according to [transform].
     *
     * The last non-error state will be passed as the parameter of [transform], which should return an updated state (or
     * null to not update the state).
     *
     * This method is thread-safe and may be called concurrently, but must block to allow only a single concurrent
     * writer of the state.
     */
    protected fun mutateState(transform: (State) -> State?) {
        contract {
            callsInPlace(transform, InvocationKind.EXACTLY_ONCE)
        }

        synchronized(this) {
            val lastState = stateFlow.value.safeState
            transform(lastState)
                ?.takeIf { it != lastState }
                ?.let { transformed ->
                    log("State -> $transformed")
                    stateFlow.value = State(transformed)
                }
        }
    }

    protected fun onError(throwable: Throwable) {
        log("Error -> $throwable")
        logException(throwable)

        errors = errors.plus(throwable)

        synchronized(this) {
            stateFlow.value = StateOrError.Error(lastState = stateFlow.value.safeState, throwable = throwable)
        }
    }

    open fun reactTo(events: Flow<Event>): Flow<Event> {
        return when (eventMergeStrategy) {
            EventMergeStrategy.LATEST -> events.transformLatest { reactTo(it) }
            EventMergeStrategy.MERGE -> events.flatMapMerge {
                flow<Event> { reactTo(it) }.catch { onError(it) }
            }
        }
    }

    /**
     * Handles the given [event], typically by mutating the current state via [mutateState] after making remote calls,
     * etc. May throw exceptions, which will be wrapped as [StateOrError.Error]s.
     */
    abstract suspend fun reactTo(event: Event)

    /**
     * Logs a [message] to the console, open to allow no-op logging in tests.
     */
    protected open fun log(message: String) {
        println("[$logTag] $message")
    }

    protected open fun logException(throwable: Throwable) {
        throwable.printStackTrace()
    }
}
