package com.dzirbel.kotify.util.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn

/**
 * Maps this [StateFlow] to another hot [StateFlow] with the given [transform], where collection of the mapped flow is
 * done in the given [scope].
 *
 * See https://github.com/Kotlin/kotlinx.coroutines/issues/2514 (among others)
 */
fun <T, R> StateFlow<T>.mapIn(scope: CoroutineScope, transform: (T) -> R): StateFlow<R> {
    val initialValue = value
    return this
        .dropWhile { it == initialValue } // hack: ensure map is not called with the initial value
        .map(transform)
        .stateIn(scope, SharingStarted.Eagerly, transform(initialValue))
}

/**
 * Flat-maps this [StateFlow] to another hot [StateFlow] with the given [transform], where collection of the mapped flow
 * is done in the given [scope].
 *
 * See https://github.com/Kotlin/kotlinx.coroutines/issues/2514 (among others)
 */
fun <T, R> StateFlow<T>.flatMapLatestIn(scope: CoroutineScope, transform: (T) -> StateFlow<R>): StateFlow<R> {
    val initialValue = value
    val initialTransform = transform(initialValue)

    // hack: avoid calling transform until a value different from the initial one is emitted to avoid transform being
    // called with the initial value twice
    var dropping = true

    return this
        .flatMapLatest { value ->
            if (dropping && value == initialValue) {
                initialTransform
            } else {
                dropping = false
                transform(value)
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, initialTransform.value)
}

/**
 * Applies [action] to each value emitted by this [StateFlow], where collection of the mapped flow is done in the given
 * [scope].
 *
 * See https://github.com/Kotlin/kotlinx.coroutines/issues/2514 (among others)
 */
fun <T> StateFlow<T>.onEachIn(scope: CoroutineScope, action: (T) -> Unit): StateFlow<T> {
    val initialValue = value
    return this
        .dropWhile { it == initialValue } // hack: ensure action is not called with the initial value twice
        .onEach(action)
        .stateIn(scope, SharingStarted.Eagerly, value.also(action))
}

/**
 * Returns a [StateFlow] with by iteratively apply [operation] to the values of this [StateFlow] and emitted each
 * result, where collection of the mapped flow is done in the given [scope].
 *
 * See https://github.com/Kotlin/kotlinx.coroutines/issues/2514 (among others)
 */
fun <T> StateFlow<T>.runningFoldIn(scope: CoroutineScope, operation: (accumulator: T, value: T) -> T): StateFlow<T> {
    val initialValue = value
    return this
        .dropWhile { it == initialValue } // hack: ensure action is not called with the initial value twice
        .runningFold(initial = initialValue, operation = operation)
        .stateIn(scope, SharingStarted.Eagerly, initialValue)
}
