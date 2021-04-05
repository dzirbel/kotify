package com.dominiczirbel.ui.util

import com.dominiczirbel.ui.util.RemoteState.Error
import com.dominiczirbel.ui.util.RemoteState.Loading
import com.dominiczirbel.ui.util.RemoteState.Success

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
    data class Error<T : Any?>(val throwable: Throwable) : RemoteState<T>()

    /**
     * The [RemoteState] when the remote call has successfully returned with [data].
     */
    data class Success<T : Any?>(val data: T) : RemoteState<T>()
}
