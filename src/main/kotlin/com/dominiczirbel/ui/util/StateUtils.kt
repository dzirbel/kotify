package com.dominiczirbel.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProduceStateScope
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Collects the [callback] value as a [State], initially null, which is assigned a value when [callback] returns.
 *
 * The result of [callback] is [remember]ed as long as [key] is unchanged and only called once as long as the caller is
 * in the composition.
 */
@Composable
fun <T> callbackAsState(
    context: CoroutineContext = EmptyCoroutineContext,
    key: Any,
    callback: suspend () -> T?
): State<T?> {
    return produceState(initialValue = null, key1 = key) {
        if (context == EmptyCoroutineContext) {
            callback()?.let { this@produceState.value = it }
        } else {
            withContext(context) {
                callback()?.let { this@produceState.value = it }
            }
        }
    }
}

/**
 * Collects values from this [Flow] and represents its latest value via [State].
 *
 * Unlike [collectAsState], this function is capable of switching between different input [Flow]s via [key]; that is, if
 * [key] ever changes, the [initial] value will be fetched again and used. [collectAsState] does not change its state
 * when the underlying [Flow] changes, only when it emits new values.
 */
@Composable
fun <T> Flow<T>.collectAsStateSwitchable(
    initial: () -> T,
    key: Any?,
    context: CoroutineContext = Dispatchers.IO
): State<T> {
    // copied from internal compose code
    class ProduceStateScopeImpl<T>(state: MutableState<T>, override val coroutineContext: CoroutineContext) :
        ProduceStateScope<T>, MutableState<T> by state {

        override suspend fun awaitDispose(onDispose: () -> Unit): Nothing {
            try {
                suspendCancellableCoroutine<Nothing> { }
            } finally {
                onDispose()
            }
        }
    }

    val result = remember(key) { mutableStateOf(initial()) }
    LaunchedEffect(this, context) {
        ProduceStateScopeImpl(result, coroutineContext).run {
            if (context == EmptyCoroutineContext) {
                collect { value = it }
            } else {
                withContext(context) {
                    collect { value = it }
                }
            }
        }
    }
    return result
}

/**
 * Handles the three possible states of [state]: an exception is thrown, in which case [onError] is invoked with the
 * exception; null is returned, in which case [onLoading] is invoked; or a non-null [T] is returned in which case
 * [onSuccess] with the value.
 */
@Composable
fun <T> HandleState(
    state: @Composable () -> T?,
    onError: @Composable (Throwable) -> Unit,
    onLoading: @Composable () -> Unit,
    onSuccess: @Composable (T) -> Unit
) {
    val result = runCatching { state() }
    val throwable = result.exceptionOrNull()

    if (throwable == null) {
        result.getOrNull()?.let { onSuccess(it) } ?: onLoading()
    } else {
        onError(throwable)
    }
}
