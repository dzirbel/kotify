package com.dzirbel.kotify.db

import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.File

/**
 * A JUnit test extension which initializes the [KotifyDatabase] for each test, sets [KotifyDatabase.enabled] to true
 * while tests are running, and deletes all database contents after each test.
 */
class DatabaseExtension : BeforeAllCallback, BeforeEachCallback, AfterEachCallback {

    override fun beforeAll(context: ExtensionContext) {
        KotifyDatabase.init(dbDir = File("."), tempFile = true)
    }

    override fun beforeEach(context: ExtensionContext) {
        KotifyDatabase.enabled = true
    }

    override fun afterEach(context: ExtensionContext) {
        KotifyDatabase.deleteAll()
        KotifyDatabase.enabled = false
    }
}
