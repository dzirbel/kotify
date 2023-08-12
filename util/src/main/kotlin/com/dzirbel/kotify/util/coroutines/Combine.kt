package com.dzirbel.kotify.util.coroutines

import com.dzirbel.kotify.util.collections.mapIfAllNotNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import java.util.concurrent.CancellationException

/**
 * Convenience wrapper around [combine] with the flows as a receiver parameter.
 */
inline fun <reified T, R> List<Flow<T>>.combine(crossinline transform: suspend (Array<T>) -> R) =
    combine(this, transform)

/**
 * Combines these [StateFlow]s into a single [StateFlow] whose value reflects [transform] applied to the latest set of
 * values from the input [StateFlow]s.
 *
 * TODO avoid double call to [transform] on the initial collection, if possible
 *
 * @see combine
 */
inline fun <reified T, R> List<StateFlow<T>>.combineState(crossinline transform: (Array<T>) -> R): StateFlow<R> {
    val stateFlows = this
    return object : StateFlow<R> {
        override var value: R = transform(Array(stateFlows.size) { stateFlows[it].value })
            private set // no-op but should not be set outside the collection

        override val replayCache: List<R>
            get() = listOf(value)

        override suspend fun collect(collector: FlowCollector<R>): Nothing {
            // note: directly passing through `transform = transform` throws a NullPointerException as of Kotlin 1.8.20,
            // which appears to be a bug related to `crossinline` parameters
            combine(flows = stateFlows, transform = { transform(it) })
                .collect { r ->
                    value = r
                    collector.emit(r)
                }

            throw CancellationException("finished collection of combined StateFlows")
        }
    }
}

/**
 * Combines these [StateFlow]s into a single [StateFlow] which reflects [transform] applied to each flow, and whose
 * combined value is only present when all the transformed values are non-null.
 *
 * Typically used to combine a [StateFlow] which emits only once the first time a set of values are loaded rather than
 * each time a new one is available.
 */
inline fun <reified T, R : Any> List<StateFlow<T>>.combinedStateWhenAllNotNull(
    noinline transform: (T) -> R?,
): StateFlow<List<R>?> {
    return combineState { array -> array.mapIfAllNotNull(transform) }
}
