package com.dzirbel.kotify.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
 * Maps this [StateFlow] to another hot [StateFlow] with the given [mapper] with collection of the mapped flow being
 * done in the given [scope].
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
