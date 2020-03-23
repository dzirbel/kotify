package com.dominiczirbel

import org.junit.jupiter.api.fail

/**
 * A variant of [org.junit.jupiter.api.assertThrows] which runs [block] inline so that it can be suspended.
 */
inline fun <reified E : Throwable> assertThrowsInline(block: () -> Unit): E {
    try {
        block()
    } catch (ex: Throwable) {
        if (ex is E) {
            return ex
        } else {
            fail { "Expected exception of type ${E::class.java.name}, but got ${ex::class.java.name}: ${ex.message}" }
        }
    }

    fail { "Expected exception of type ${E::class.java.name}, but nothing was thrown" }
}
