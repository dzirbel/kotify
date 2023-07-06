package com.dzirbel.kotify.repository.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A simple utility which [launch]es work only when the previous job launched has finished.
 *
 * This can be used for e.g. fetching some remote state where it's unnecessary to start another fetch when one is
 * already in progress.
 */
internal class JobLock {
    private val running = AtomicBoolean(false)

    /**
     * Launches [block] as a job in [scope] if there is no other job currently holding the lock.
     */
    fun launch(scope: CoroutineScope, block: suspend CoroutineScope.() -> Unit) {
        if (!running.getAndSet(true)) {
            scope.launch {
                try {
                    block()
                } finally {
                    running.set(false)
                }
            }
        }
    }

    /**
     * Throws an [IllegalStateException] if there is currently a job running from [launch].
     */
    fun checkNotRunning() {
        check(!running.get())
    }
}
