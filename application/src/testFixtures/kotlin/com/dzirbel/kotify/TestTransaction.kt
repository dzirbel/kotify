package com.dzirbel.kotify

import com.dzirbel.kotify.db.KotifyDatabase
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Transaction

/**
 * Simple blocking wrapper around [KotifyDatabase.transaction] for use in tests.
 *
 * TODO remove / move to :db?
 */
fun <T> testTransaction(statement: suspend Transaction.() -> T): T {
    return runBlocking {
        KotifyDatabase.transaction(name = null, statement = statement)
    }
}
