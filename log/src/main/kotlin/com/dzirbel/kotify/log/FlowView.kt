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
data class FlowView<T>(
    val filter: ((T) -> Boolean)? = null,
    val sort: Comparator<T>? = null,
) {
    /**
     * Returns a [StateFlow] which reflects the filtered and sorted emissions of the [flow].
     *
     * @param flow the [Flow] whose emissions are accumulated into a list
     * @param initial the initial list of events to start with
     * @param scope the [CoroutineScope] in which the [flow] is collected
     */
    fun viewState(flow: Flow<T>, initial: Iterable<T>, scope: CoroutineScope): StateFlow<ImmutableList<T>> {
        val stateFlow = MutableStateFlow(
            value = initial
                .let { if (filter == null) it else it.filter(filter) }
                .let { if (sort == null) it else it.sortedWith(sort) }
                .toPersistentList(),
        )

        // use an undispatched start to attempt to start collection immediately and avoid missing any events emitted
        // between the read of the initial list and the start of collection; this is not quite airtight but unlikely to
        // be an issue in practice
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            flow.collect { event ->
                if (filter == null || filter.invoke(event)) {
                    stateFlow.value =
                        if (sort == null) stateFlow.value.add(event) else stateFlow.value.addSorted(event, sort)
                }
            }
        }

        return stateFlow
    }

    /**
     * Returns a [FlowView] for values of type [R] applies this [FlowView]'s [filter] and [sort] on the values of type
     * [R] by transforming them via [transform].
     */
    fun <R> transformed(transform: (R) -> T): FlowView<R> {
        return FlowView(
            filter = filter?.let { filter -> { event: R -> filter(transform(event)) } },
            sort = sort?.let { sort -> Comparator.comparing(transform, sort) },
        )
    }
}
