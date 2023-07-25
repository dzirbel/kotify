package com.dzirbel.kotify.db

import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A JUnit test extension which setups up the database connection and clears database contents after each test.
 *
 * This extension is applied automatically to all tests via a service loader.
 */
class DatabaseExtension : BeforeAllCallback, AfterEachCallback {
    private val initializedDb = AtomicBoolean(false)

    override fun beforeAll(context: ExtensionContext) {
        // only initialize the DB once per test suite (whereas beforeAll is called once per test class)
        if (!initializedDb.getAndSet(true)) {
            // use database file relative to the module to avoid interference with parallel test suites
            KotifyDatabase.init(dbFile = File("test.db").also(File::deleteOnExit))
        }
    }

    override fun afterEach(context: ExtensionContext) {
        KotifyDatabase.deleteAll()
    }
}
