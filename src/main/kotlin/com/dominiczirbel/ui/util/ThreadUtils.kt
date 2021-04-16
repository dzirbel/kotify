package com.dominiczirbel.ui.util

/**
 * Asserts that the current thread is not a UI thread.
 */
fun assertNotOnUIThread() {
    assert(!Thread.currentThread().name.contains("AWT-EventQueue")) {
        "Should not have been on UI thread, but was on ${Thread.currentThread()}"
    }
}
