package com.dzirbel.kotify.util.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

/**
 * A [CoroutineDispatcher] backed by an unbounded number of virtual threads for each task.
 *
 * Use of this [CoroutineDispatcher] for composition-local and repository work is recommended to avoid overwhelming the
 * thread pool of [Dispatchers.IO], which appears to also be used by the Compose runtime (so it may block UI rendering).
 */
val Dispatchers.Computation: CoroutineDispatcher by lazy {
    Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()
}
