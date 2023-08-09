package com.dzirbel.kotify.db

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Transaction
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Blocking wrapper around [KotifyDatabase.transaction] for use in tests.
 */
fun <T> KotifyDatabase.blockingTransaction(statement: Transaction.() -> T): T {
    contract {
        callsInPlace(statement, InvocationKind.EXACTLY_ONCE)
    }

    return withSynchronousTransactions {
        runBlocking {
            transaction(name = null, statement = statement)
        }
    }
}
