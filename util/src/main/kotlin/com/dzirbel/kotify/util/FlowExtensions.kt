package com.dzirbel.kotify.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance

/**
 * Returns a [Flow] of type [T] which emits no values, but will trigger collection of the base [Flow].
 */
inline fun <reified T> Flow<*>.ignore(): Flow<T> {
    return filter { false }.filterIsInstance()
}
