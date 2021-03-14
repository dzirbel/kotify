package com.dominiczirbel.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.dominiczirbel.ui.util.RemoteState.Error
import com.dominiczirbel.ui.util.RemoteState.Loading
import com.dominiczirbel.ui.util.RemoteState.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlin.coroutines.CoroutineContext

/**
 * A common wrapper around [androidx.compose.runtime.MutableState], which allows easy asynchronous loading of state and
 * common wrappers for [Loading], [Error], and [Success] conditions.
 */
sealed class RemoteState<T : Any> {
    /**
     * The initial [RemoteState] where the remote call has not yet returned.
     */
    class Loading<T : Any> : RemoteState<T>()

    /**
     * A [RemoteState] triggered when the remote call threw a [throwable] exception.
     */
    class Error<T : Any>(val throwable: Throwable) : RemoteState<T>()

    /**
     * The [RemoteState] when the remote call has successfully returned with [data].
     */
    class Success<T : Any>(val data: T) : RemoteState<T>()

    companion object {
        /**
         * Generates a [RemoteState] from the given [remote] call.
         *
         * Successive calls to this function will return updated states, as the remote may have completed/errored. The
         * remote call is [remember]ed, so only the first call in a recomposition will trigger loading.
         *
         * The [remote] call can be re-fetched by emitting an [Unit] from the given [sharedFlow].
         */
        @Composable
        fun <T : Any> of(
            sharedFlow: MutableSharedFlow<Unit> = MutableSharedFlow(),
            context: CoroutineContext = Dispatchers.IO,
            remote: suspend () -> T
        ): RemoteState<T> {
            return remember {
                sharedFlow
                    .onStart { emit(Unit) }
                    .flatMapLatest { flowOf(remote()) }
                    .map { Success(it) }
                    .catch { Error<T>(it) }
            }.collectAsState(initial = Loading(), context = context).value
        }
    }
}
