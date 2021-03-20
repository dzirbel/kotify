package com.dominiczirbel.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.dominiczirbel.ui.util.RemoteState.Error
import com.dominiczirbel.ui.util.RemoteState.Loading
import com.dominiczirbel.ui.util.RemoteState.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlin.coroutines.CoroutineContext

/**
 * A common wrapper around [androidx.compose.runtime.MutableState], which allows easy asynchronous loading of state and
 * common wrappers for [Loading], [Error], and [Success] conditions.
 */
sealed class RemoteState<T : Any?> {
    /**
     * The initial [RemoteState] where the remote call has not yet returned.
     */
    class Loading<T : Any?> : RemoteState<T>()

    /**
     * A [RemoteState] triggered when the remote call threw a [throwable] exception.
     */
    class Error<T : Any?>(val throwable: Throwable) : RemoteState<T>()

    /**
     * The [RemoteState] when the remote call has successfully returned with [data].
     */
    class Success<T : Any?>(val data: T) : RemoteState<T>()

    companion object {
        @Composable
        fun <T : Any?> of(
            sharedFlow: MutableSharedFlow<Unit> = MutableSharedFlow(),
            key: Any? = null,
            context: CoroutineContext = Dispatchers.IO,
            remote: suspend () -> T
        ): RemoteState<T> {
            return of(
                sharedFlow = sharedFlow,
                initial = Unit,
                key = key,
                context = context,
                remote = { _, _ -> remote() }
            )
        }

        /**
         * Generates a [RemoteState] from the given [remote] call.
         *
         * Successive calls to this function will return updated states, as the remote may have completed/errored. The
         * remote call is [remember]ed, so only the first call in a recomposition will trigger loading.
         *
         * The [remote] call can be re-fetched by emitting an [Unit] from the given [sharedFlow].
         */
        @Composable
        fun <T : Any?, R : Any> of(
            sharedFlow: MutableSharedFlow<R> = MutableSharedFlow(),
            initial: R,
            key: Any? = null,
            context: CoroutineContext = Dispatchers.IO,
            remote: suspend (T?, R) -> T
        ): RemoteState<T> {
            return remember(key) {
                sharedFlow
                    .onStart { emit(initial) }
                    // TODO doesn't switch to latest if there are multiple queued events
                    .scan(null) { acc: T?, value: R -> remote(acc, value) }
                    .mapNotNull<T?, RemoteState<T>> { it?.let { Success(it) } }
                    .catch {
                        it.printStackTrace()
                        emit(Error(it))
                    }
            }.collectAsStateSwitchable(initial = { Loading() }, context = context, key = key).value
        }
    }
}
