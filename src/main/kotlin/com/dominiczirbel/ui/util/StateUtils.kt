package com.dominiczirbel.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.CoroutineContext

/**
 * Collects the [callback] value as a [State], initially null, which is assigned a value when [callback] returns.
 *
 * The result of [callback] is [remember]ed and only called once as long as the caller is in the composition.
 */
@Composable
fun <T> callbackAsState(context: CoroutineContext = Dispatchers.IO, callback: suspend () -> T?): State<T?> {
    return remember {
        flow {
            callback()?.let { emit(it) }
        }
    }.collectAsState(initial = null, context = context)
}

/**
 * Collects the [callback] value as a [State], initially null, which is assigned a value when [callback] returns.
 *
 * The result of [callback] is [remember]ed as long as [key] is unchanged and only called once as long as the caller is
 * in the composition.
 */
@Composable
fun <T> callbackAsState(context: CoroutineContext = Dispatchers.IO, key: Any, callback: suspend () -> T?): State<T?> {
    return remember(key) {
        flow {
            callback()?.let { emit(it) }
        }
    }.collectAsState(initial = null, context = context)
}
