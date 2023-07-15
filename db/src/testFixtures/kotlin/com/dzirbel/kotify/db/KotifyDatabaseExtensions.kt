package com.dzirbel.kotify.db

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Transaction

/**
 * Blocking wrapper around [KotifyDatabase.transaction] for use in tests.
 */
fun <T> KotifyDatabase.blockingTransaction(statement: Transaction.() -> T): T {
    return withSynchronousTransactions {
        runBlocking {
            transaction(name = null, statement = statement)
        }
    }
}
