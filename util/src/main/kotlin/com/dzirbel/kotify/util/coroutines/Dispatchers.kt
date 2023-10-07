package com.dzirbel.kotify.util.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

/**
 * A [CoroutineDispatcher] backed by a pool of cached threads with unlimited size.
 *
 * Use of this [CoroutineDispatcher] for composition-local and repository work is recommended to avoid overwhelming the
 * thread pool of [Dispatchers.IO], which appears to also be used by the Compose runtime (so it may block UI rendering).
 */
val Dispatchers.Computation: CoroutineDispatcher by lazy {
    // TODO use virtual thread pool in Java 21
    Executors.newCachedThreadPool().asCoroutineDispatcher()
}
