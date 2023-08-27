package com.dzirbel.kotify.log

import com.dzirbel.kotify.util.immutable.addSorted
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Wraps [filter] and [sort] operations which can be applied to a [Flow] to produce a [StateFlow] which accumulates the
 * filtered and sorted emissions of the [Flow].
 */
data class FlowView<E>(
    val filter: ((E) -> Boolean)? = null,
    val sort: Comparator<E>? = null,
) {
    /**
     * Returns a [StateFlow] which reflects the filtered and sorted emissions of the [flow].
     *
     * @param flow the [Flow] whose emissions are accumulated into a list
     * @param initial the initial list of events to start with
     * @param scope the [CoroutineScope] in which the [flow] is collected
     */
    fun viewState(flow: Flow<E>, initial: List<E>, scope: CoroutineScope): StateFlow<ImmutableList<E>> {
        val stateFlow = MutableStateFlow(
            value = initial
                .let { if (filter == null) it else it.filter(filter) }
                .let { if (sort == null) it else it.sortedWith(sort) }
                .toPersistentList(),
        )

        // use an undispatched start to attempt to start collection immediately and avoid missing any events emitted
        // between the read of the initial list and the start of collection; this is not airtight but reasonable
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            flow.collect { event ->
                if (filter?.invoke(event) != false) {
                    stateFlow.value =
                        if (sort == null) stateFlow.value.add(event) else stateFlow.value.addSorted(event, sort)
                }
            }
        }

        return stateFlow
    }
}

/**
 * Convenience wrapper on [FlowView.viewState] which observes the events of the given [log] and returns a [StateFlow]
 * which reflects the filtered and sorted events of the [log].
 */
fun <E : Log.Event> FlowView<E>.viewState(log: Log<E>, scope: CoroutineScope): StateFlow<ImmutableList<E>> {
    return viewState(flow = log.eventsFlow, initial = log.events, scope = scope)
}
