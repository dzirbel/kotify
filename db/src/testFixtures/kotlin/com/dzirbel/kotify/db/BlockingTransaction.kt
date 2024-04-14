package com.dzirbel.kotify.db

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Transaction
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Blocking wrapper around [KotifyDatabase.DatabaseContext.transaction] for use in tests.
 */
@Suppress("LEAKED_IN_PLACE_LAMBDA", "WRONG_INVOCATION_KIND") // false positives (probably) in Kotlin 2.0.0-RC1
fun <T> KotifyDatabase.blockingTransaction(db: DB = DB.CACHE, statement: Transaction.() -> T): T {
    contract {
        callsInPlace(statement, InvocationKind.EXACTLY_ONCE)
    }

    val databaseContext = this[db]
    return withSynchronousTransactions {
        runBlocking {
            databaseContext.transaction(name = null, statement = statement)
        }
    }
}
