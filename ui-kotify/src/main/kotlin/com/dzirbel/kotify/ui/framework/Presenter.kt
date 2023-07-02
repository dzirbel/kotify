package com.dzirbel.kotify.ui.framework

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.dzirbel.kotify.Logger
import com.dzirbel.kotify.ui.framework.Presenter.StateOrError.State
import com.dzirbel.kotify.ui.util.assertNotOnUIThread
import com.dzirbel.kotify.ui.util.collectAsStateSwitchable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KClass

/**
 * Convenience function which manages the creation of a [Presenter] tied to the composition.
 */
@Composable
fun <ViewModel, P : Presenter<ViewModel, *>> rememberPresenter(
    key: Any? = Unit,
    createPresenter: (CoroutineScope) -> P,
): P {
    return key(key) {
        val scope = rememberCoroutineScope { Dispatchers.IO }
        remember { createPresenter(scope) }
    }
}

/**
 * A presenter abstraction which controls the state of a particular piece of the UI.
 *
 * The presenter continually listens for [emit]ted events and processes them via [reactTo], which calls [mutateState] to
 * update the view [state].
 *
 * TODO rework or replace presenter
 */
@Stable
abstract class Presenter<ViewModel, Event : Any>(
    /**
     * The [CoroutineScope] under which this presenter operates, typically bound to the UI's point in the composition
     * (i.e. from [androidx.compose.runtime.rememberCoroutineScope]).
     */
    protected val scope: CoroutineScope,

    /**
     * The initial [ViewModel] of the content.
     */
    private val initialState: ViewModel,

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
    private val eventMergeStrategy: EventMergeStrategy = EventMergeStrategy.LATEST_BY_CLASS,

    /**
     * An optional list of events which should be emitted at the beginning of the event flow, e.g. to load content.
     */
    private val startingEvents: List<Event>? = null,
) {
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
        MERGE,

        /**
         * Only the latest event of each class is processed; processing of previous events of a particular class is
         * cancelled when a new event of that class arrives. Events of different types are processed concurrently.
         */
        LATEST_BY_CLASS,
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
     * A [Throwable] representing an error indicating a resource was not found.
     *
     * If used as the [StateOrError.Error.throwable] (i.e. thrown during presenter processing) it should be displayed in
     * a user-friendly way that a particular page could not be found.
     */
    class NotFound(message: String = "Not found") : Throwable(message)

    /**
     * A [MutableStateFlow] which exposes the current state (via the [StateOrError] wrapper, possibly wrapping an
     * exception instead). Should only be modified internally, and writes must be synchronized.
     *
     * Ideally we might use a simpler mechanism to represent the state, but e.g. [androidx.compose.runtime.MutableState]
     * does not allow writes during a snapshot and so cannot support arbitrary concurrency.
     */
    private val stateFlow = MutableStateFlow<StateOrError<ViewModel>>(State(initialState))

    /**
     * Exposes the current state for tests, where the composable [state] cannot be called.
     */
    val testState: StateOrError<ViewModel>
        get() = synchronized(this) { stateFlow.value }

    private val stateCount = AtomicInteger(0)
    private val eventCount = AtomicInteger(0)

    private val events = MutableSharedFlow<Event>()

    private val errorsFlow = MutableStateFlow<List<Throwable>>(emptyList())

    /**
     * A list of accumulated errors from the event stream.
     */
    var errors: List<Throwable>
        get() = errorsFlow.value
        set(value) {
            errorsFlow.value = value
        }

    /**
     * The core event [Flow], which includes emitting [startingEvents], reacting to events according to the
     * [eventMergeStrategy] with [reactTo], and catching errors.
     *
     * This must be its own function to allow restarting the event flow on error by making a recursive call to
     * [eventFlow].
     */
    private fun eventFlow(startingEvents: List<Event>? = this.startingEvents): Flow<Event> {
        return (externalEvents()?.let { merge(it, events) } ?: events)
            .onStart { startingEvents?.forEach { emit(it) } }
            .onEach { event ->
                assertNotOnUIThread()
                Logger.UI.handleEvent(presenter = this, event = event, eventCount = eventCount.incrementAndGet())
            }
            .let { flow -> reactTo(flow) }
            .catch { throwable ->
                onError(throwable)
                emitAll(eventFlow(null))
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
    suspend fun open() {
        eventFlow().collect()
    }

    /**
     * Optionally returns a [Flow]s of [Event]s which are incorporated into the event stream. This allows the presenter
     * to react to streams of external data in the same way as events from [emit].
     */
    open fun externalEvents(): Flow<Event>? = null

    /**
     * Listens and handles events for this presenter and returns its current state. This function is appropriate to be
     * called in a [Composable] function and returns a composition-aware state.
     */
    @Composable
    fun state(context: CoroutineContext = EmptyCoroutineContext): StateOrError<ViewModel> {
        remember(key) {
            scope.launch(context = context) {
                open()
            }
        }

        return stateFlow.collectAsStateSwitchable(
            initial = { State(initialState) },
            key = key,
            context = context,
        ).value
    }

    /**
     * Clears any current error state from this presenter, resetting it the last non-error state.
     */
    fun clearError() {
        synchronized(this) {
            stateFlow.value = State(stateFlow.value.safeState)
        }
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
    protected fun mutateState(transform: (ViewModel) -> ViewModel?) {
        contract {
            callsInPlace(transform, InvocationKind.EXACTLY_ONCE)
        }

        synchronized(this) {
            val lastState = stateFlow.value.safeState
            transform(lastState)
                ?.takeIf { it != lastState }
                ?.let { transformed ->
                    Logger.UI.handleState(
                        presenter = this,
                        state = transformed,
                        stateCount = stateCount.incrementAndGet(),
                    )
                    stateFlow.value = State(transformed)
                }
        }
    }

    /**
     * Fetches the current [state] in order use some of its values for processing without immediately mutating it.
     *
     * The last non-error state will be passed as the parameter of [transform].
     *
     * This method is thread-safe and may be called concurrently, but must block to avoid concurrent writes with
     * [mutateState].
     */
    protected fun <T> queryState(transform: (ViewModel) -> T): T = transform(stateFlow.value.safeState)

    private fun onError(throwable: Throwable) {
        Logger.UI.handleError(presenter = this, throwable = throwable)

        errorsFlow.value = errorsFlow.value.plus(throwable)

        synchronized(this) {
            stateFlow.value = StateOrError.Error(lastState = stateFlow.value.safeState, throwable = throwable)
        }
    }

    /**
     * Handles the given flow of [events], by default according to the [eventMergeStrategy] and [reactTo] for each
     * event in the flow.
     */
    fun reactTo(events: Flow<Event>): Flow<Event> {
        return when (eventMergeStrategy) {
            EventMergeStrategy.LATEST_BY_CLASS -> {
                // map event classes to the MutableSharedFlow on which events of that class are emitted
                val mutableSharedFlowMap = mutableMapOf<KClass<out Event>, MutableSharedFlow<Event>>()

                events.flatMapMerge { event ->
                    val eventClass = event::class

                    synchronized(mutableSharedFlowMap) {
                        if (mutableSharedFlowMap.containsKey(eventClass)) {
                            // if this event class has already been included in the flatMap, we need to avoid merging it
                            // in again (but still need to emit to its MutableSharedFlow below)
                            emptyFlow()
                        } else {
                            // if this is the first event of this class, create a new MutableSharedFlow to contain its
                            // events; use a replay to avoid missing the first event which is emitted below
                            MutableSharedFlow<Event>(replay = 1)
                                .also { mutableSharedFlowMap[eventClass] = it }
                                .transformLatest<Event, Event> { reactTo(it) }
                        }
                    }
                        .also {
                            // finally, emit the event to the MutableSharedFlow which handles its event class
                            requireNotNull(mutableSharedFlowMap[eventClass]) {
                                "missing MutableSharedFlow for $eventClass"
                            }
                                .emit(event)
                        }
                }
            }
            EventMergeStrategy.LATEST -> events.transformLatest { reactTo(it) }
            EventMergeStrategy.MERGE -> events.flatMapMerge { event ->
                flow<Event> { reactTo(event) }.catch { onError(it) }
            }
        }
    }

    /**
     * Handles the given [event], typically by mutating the current state via [mutateState] after making remote calls,
     * etc. May throw exceptions, which will be wrapped as [StateOrError.Error]s.
     */
    abstract suspend fun reactTo(event: Event)
}
