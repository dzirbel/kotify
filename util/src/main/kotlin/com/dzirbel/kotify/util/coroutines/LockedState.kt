package com.dzirbel.kotify.util.coroutines

import com.dzirbel.kotify.util.collections.sortedIndexFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Creates a [StateFlow] which reflects the state of a resource locked by this [Mutex], created while the lock is held
 * from [initializeWithLock] and [flow] to produce the initial value and a flow of values collected into the
 * [StateFlow].
 *
 * This is meant for resources such as logs which expose both a current state (e.g. list of events) and a stream of
 * updates (e.g. newly logged events) where it would otherwise be possible for events to be lost between when the
 * initial state is read and the collection of updates is started. By locking the resource while the flow is initialized
 * and up to just the moment when collection is started we can very nearly guarantee that no values are lost.
 *
 * @param T type of values produced by the [StateFlow]
 * @param initial an initial value used as the [StateFlow] value while the lock is being acquired. As such, this should
 *  not be based on a state read of the locked resource, and in many cases (i.e. when the lock is available) will not be
 *  reflected in the [StateFlow] at all
 * @param scope a [CoroutineScope] in which the [flow] is collected
 * @param initializeWithLock an optional initialization callback replacing the [initial] [StateFlow] value once the lock
 *  has been acquired. If this initialization is expensive and [start] is undispatched, consider yielding the coroutine
 *  execution so that it is done on the given [context]
 * @param owner an optional owner for debugging provided to [Mutex.lock]
 * @param context a [CoroutineContext] with which to collect the [flow]
 * @param start strategy for starting acquisition of the [Mutex] lock; by default [CoroutineStart.UNDISPATCHED] so that
 *  the lock is attempted to be acquired immediately, such that the returned [StateFlow] reflects the value from
 *  [initializeWithLock] rather than [initial]
 * @param flow generates a [Flow] of values collected into the returned [StateFlow] once the lock has been acquired,
 *  given the initial value, either [initial] or the result of [initializeWithLock] if provided
 */
fun <T> Mutex.lockedState(
    initial: T,
    scope: CoroutineScope,
    initializeWithLock: (suspend () -> T)? = null,
    owner: Any? = null,
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.UNDISPATCHED,
    flow: suspend (initial: T) -> Flow<T>,
): StateFlow<T> {
    val stateFlow = MutableStateFlow(initial)
    scope.launch(context = context, start = start) {
        withLock(owner = owner) {
            if (initializeWithLock == null) {
                flow(initial)
            } else {
                val initialized = initializeWithLock()
                stateFlow.value = initialized
                flow(initialized)
            }
        }
            .collect { stateFlow.value = it }
    }
    return stateFlow
}

/**
 * A convenience variant of [lockedState] which initializes the flow to null but requires a proper value be provided
 * by [initializeWithLock] when the lock has been acquired.
 */
fun <T : Any> Mutex.lockedState(
    scope: CoroutineScope,
    initializeWithLock: suspend () -> T,
    owner: Any? = null,
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.UNDISPATCHED,
    flow: suspend (initial: T) -> Flow<T>,
): StateFlow<T?> {
    return lockedState(
        initial = null,
        scope = scope,
        initializeWithLock = initializeWithLock,
        owner = owner,
        context = context,
        start = start,
        flow = { flow(requireNotNull(it)) },
    )
}

/**
 * A variant of [lockedState] which generates a [StateFlow] of a [List] of elements, optionally applying a [sort] and
 * [filter] as necessary (i.e. without re-sorting/re-filtering the entire list on each new element).
 */
fun <T> Mutex.lockedListState(
    scope: CoroutineScope,
    initial: List<T> = emptyList(),
    sort: Comparator<T>? = null,
    filter: ((T) -> Boolean)? = null,
    owner: Any? = null,
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.UNDISPATCHED,
    initializeWithLock: suspend () -> Iterable<T>,
    flow: suspend (initial: List<T>) -> Flow<T>,
): StateFlow<List<T>> {
    return lockedState(
        initial = initial,
        scope = scope,
        initializeWithLock = {
            initializeWithLock()
                .let { if (filter == null) it else it.filter(filter) }
                .let { if (sort == null) it else it.sortedWith(sort) }
                .toMutableList()
        },
        owner = owner,
        context = context,
        start = start,
        flow = { initialList ->
            @Suppress("DontDowncastCollectionTypes")
            initialList as MutableList<T>

            flow(initialList).runningFold(initialList) { list, value ->
                if (filter == null || filter.invoke(value)) {
                    if (sort == null) {
                        list.add(value)
                    } else {
                        list.add(index = list.sortedIndexFor(value, sort), element = value)
                    }
                }
                list
            }
        },
    )
}
