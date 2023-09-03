package com.dzirbel.kotify.util.coroutines

import com.dzirbel.kotify.util.collections.mapLazy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge

/**
 * Returns a [Flow] which merges the [Flow]s returned by applying [mapper] to each element of this [Iterable].
 */
fun <T, R> Iterable<T>.mergeFlows(mapper: (T) -> Flow<R>): Flow<R> {
    return mapLazy(mapper).merge()
}
