package com.dominiczirbel.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

// TODO document
// TODO test
sealed class RemoteState<T : Any> {
    class Loading<T : Any> : RemoteState<T>()

    class Error<T : Any>(val throwable: Throwable) : RemoteState<T>()

    class Success<T : Any>(val data: T) : RemoteState<T>()

    companion object {
        @Composable
        fun <T : Any> of(
            context: CoroutineContext = Dispatchers.IO,
            remote: suspend () -> T
        ): RemoteState<T> {
            val state: MutableState<RemoteState<T>> = remember { mutableStateOf(Loading()) }

            if (state.value is Loading<*>) {
                @Suppress("GlobalCoroutineUsage")
                GlobalScope.launch {
                    withContext(context) {
                        @Suppress("TooGenericExceptionCaught")
                        val result: RemoteState<T> = try {
                            Success(remote())
                        } catch (e: Throwable) {
                            Error(e)
                        }

                        @Suppress("MagicNumber")
                        Thread.sleep(250) // TODO: hack to avoid setting the state before the snapshot has been applied

                        state.value = result
                    }
                }
            }

            return state.value
        }
    }
}
