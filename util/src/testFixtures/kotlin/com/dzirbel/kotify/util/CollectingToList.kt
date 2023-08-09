package com.dzirbel.kotify.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent

/**
 * Concurrently collects [flow] to a live [List] passed to [block] while [block] is executed.
 *
 * This is convenient to monitor the emissions of a [Flow] while running some test logic in [block] when the collection
 * of [flow] would be blocking (or never complete). Scoped to [TestScope] to ensure it is not used in other scopes.
 */
fun <T> TestScope.collectingToList(flow: Flow<T>, block: (List<T>) -> Unit) {
    val list = mutableListOf<T>()
    val job = launch { flow.collect(list::add) }
    runCurrent() // start collection immediately

    block(list)

    job.cancel()
}
