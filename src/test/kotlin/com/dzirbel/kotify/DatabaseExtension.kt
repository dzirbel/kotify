package com.dzirbel.kotify

import com.dzirbel.kotify.db.KotifyDatabase
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * A JUnit test extension which setups up the database connection and clears database contents after each test.
 */
class DatabaseExtension : BeforeAllCallback, AfterEachCallback {
    override fun beforeAll(context: ExtensionContext?) {
        KotifyDatabase.db
        KotifyDatabase.dbFile.deleteOnExit()
    }

    override fun afterEach(context: ExtensionContext?) {
        KotifyDatabase.clear()
    }
}
