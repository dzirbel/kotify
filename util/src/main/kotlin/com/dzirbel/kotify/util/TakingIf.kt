package com.dzirbel.kotify.util

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Returns the result of [block] if [condition] is true, otherwise null.
 *
 * A simple alternative to [takeIf] which only computes the desired value when the condition is true. Unfortunately, it
 * cannot statically guarantee that [condition] is true within [block].
 */
inline fun <T> takingIf(condition: Boolean, block: () -> T): T? {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    return if (condition) block() else null
}
