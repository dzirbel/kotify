package com.dzirbel.kotify.util

import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * A JUnit extension which mocks the system time for each test.
 */
class MockedTimeExtension : BeforeEachCallback, AfterEachCallback {
    override fun beforeEach(context: ExtensionContext) {
        CurrentTime.startMock()
    }

    override fun afterEach(context: ExtensionContext) {
        CurrentTime.closeMock()
    }
}
