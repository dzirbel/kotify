package com.dzirbel.kotify.util

import io.mockk.MockKStubScope
import kotlinx.coroutines.delay

/**
 * Extends this [MockKStubScope] with the given [delayMs] for a more concise DSL to mock answers after a delay.
 */
infix fun <T, B> MockKStubScope<T, B>.delayed(delayMs: Long): DelayedMockKStubScope<T, B> {
    return DelayedMockKStubScope(this, delayMs)
}

/**
 * A simple wrapper on [baseStub] which adds a [delay] to mocked calls.
 */
class DelayedMockKStubScope<T, B>(
    private val baseStub: MockKStubScope<T, B>,
    private val delayMs: Long,
) {
    /**
     * Mocks the stub to return [value] after the [delayMs].
     */
    infix fun returns(value: T) {
        baseStub.coAnswers {
            delay(delayMs)
            value
        }
    }

    /**
     * Mocks the stub to return each of [values] in order after the [delayMs].
     */
    infix fun returnsMany(values: List<T>) {
        val remaining = values.toMutableList()
        baseStub.coAnswers {
            delay(delayMs)
            checkNotNull(remaining.removeFirstOrNull()) { "no more values" }
        }
    }

    fun returnsMany(vararg values: T) {
        returnsMany(values.toMutableList())
    }

    /**
     * Mocks the stub to throw [throwable] after the [delayMs].
     */
    infix fun throws(throwable: Throwable) {
        baseStub.coAnswers {
            delay(delayMs)
            throw throwable
        }
    }
}
