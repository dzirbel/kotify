package com.dzirbel.kotify.util

import io.mockk.mockkObject
import io.mockk.unmockkObject

/**
 * Builds object mocks for [objects], invokes [block], and unmocks them.
 */
inline fun withMockedObjects(vararg objects: Any, block: () -> Unit) {
    mockkObject(*objects)
    block()
    unmockkObject(*objects)
}
