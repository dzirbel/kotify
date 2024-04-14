package com.dzirbel.kotify.db

import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A JUnit test extension which initializes the [KotifyDatabase] (once across the entire test suite), sets
 * [KotifyDatabase.enabled] to true while tests are running, and deletes all database contents after each test.
 */
class DatabaseExtension : BeforeAllCallback, BeforeEachCallback, AfterEachCallback {
    override fun beforeAll(context: ExtensionContext) {
        // only initialize the DB once per test suite (whereas beforeAll is called once per test class)
        if (!initializedDb.getAndSet(true)) {
            // use database file relative to the module to avoid interference with parallel test suites
            KotifyDatabase.init(dbDir = File("."), tempFile = true)
        }
    }

    override fun beforeEach(context: ExtensionContext) {
        KotifyDatabase.enabled = true
    }

    override fun afterEach(context: ExtensionContext) {
        KotifyDatabase.deleteAll()

        KotifyDatabase.enabled = false
    }

    companion object {
        // hold in the companion object so the same object is used between instances created by @ExtendWith
        private val initializedDb = AtomicBoolean(false)
    }
}
