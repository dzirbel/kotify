package com.dzirbel.kotify.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlin.coroutines.cancellation.CancellationException

/**
 * Combines these [StateFlow]s into a single [StateFlow] whose value reflects [transform] applied to the latest set of
 * values from the input [StateFlow]s.
 *
 * @see combine
 */
inline fun <reified T, R> List<StateFlow<T>>.combineState(crossinline transform: (Array<T>) -> R): StateFlow<R> {
    val stateFlows = this
    return object : StateFlow<R> {
        override var value: R = transform(Array(stateFlows.size) { stateFlows[it].value })
            private set // no-op but should not be set outside of the collection

        override val replayCache: List<R>
            get() = listOf(value)

        override suspend fun collect(collector: FlowCollector<R>): Nothing {
            // note: directly passing through `transform = transform` throws a NullPointerException as of Kotlin 1.8.20,
            // which appears to be a bug related to `crossinline` parameters
            combine(flows = stateFlows, transform = { transform(it) })
                .onStart { collector.emit(value) }
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
 * combined value is only present when all of the transformed values are non-null.
 *
 * Typically used to combine a [StateFlow] which emits only once the first time a set of values are loaded rather than
 * each time a new one is available.
 */
inline fun <reified T, R : Any> List<StateFlow<T>>.combinedStateWhenAllNotNull(
    noinline transform: (T) -> R?,
): StateFlow<List<R>?> {
    return combineState { array -> array.mapIfAllNotNull(transform) }
}

/**
 * Maps this [StateFlow] to another hot [StateFlow] with the given [mapper], where collection of the mapped flow is done
 * in the given [scope].
 *
 * See https://github.com/Kotlin/kotlinx.coroutines/issues/2514 (among others)
 *
 * TODO unit test
 */
fun <T, R> StateFlow<T>.mapIn(scope: CoroutineScope, mapper: (T) -> R): StateFlow<R> {
    return this
        .map(mapper)
        .stateIn(scope, SharingStarted.Eagerly, mapper(value))
}

/**
 * Flat-maps this [StateFlow] to another hot [StateFlow] with the given [mapper], where collection of the mapped flow is
 * done in the given [scope].
 *
 * See https://github.com/Kotlin/kotlinx.coroutines/issues/2514 (among others)
 *
 * TODO unit test
 */
fun <T, R> StateFlow<T>.flatMapLatestIn(scope: CoroutineScope, mapper: (T) -> StateFlow<R>): StateFlow<R> {
    return this
        .flatMapLatest(mapper)
        .stateIn(scope, SharingStarted.Eagerly, mapper(value).value)
}
