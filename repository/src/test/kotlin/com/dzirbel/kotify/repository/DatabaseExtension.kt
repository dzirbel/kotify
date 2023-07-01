package com.dzirbel.kotify.repository

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.repository.user.UserRepository
import org.jetbrains.exposed.sql.transactions.transaction
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
        if (!initializedDb.getAndSet(true)) {
            // use database file relative to this module to avoid interference with parallel test suites
            KotifyDatabase.init(dbFile = File("test.db").also(File::deleteOnExit))
        }

        transaction(KotifyDatabase.db) {
            UserRepository.currentUserId.set("abc") // use mock user ID for tests
        }
    }

    override fun afterEach(context: ExtensionContext) {
        KotifyDatabase.deleteAll()
        savedRepositories.forEach { it.clearStates() }
    }
}
