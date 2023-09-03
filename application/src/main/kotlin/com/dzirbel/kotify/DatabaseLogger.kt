package com.dzirbel.kotify

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.log.Log
import com.dzirbel.kotify.log.MutableLog
import com.dzirbel.kotify.log.asLog
import com.dzirbel.kotify.log.info
import kotlinx.coroutines.GlobalScope
import org.jetbrains.exposed.sql.SqlLogger
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.statements.StatementInterceptor
import org.jetbrains.exposed.sql.statements.expandArgs
import kotlin.time.Duration.Companion.milliseconds

object DatabaseLogger : KotifyDatabase.TransactionListener, SqlLogger, StatementInterceptor {

    // map from transaction id to (name, statements)
    private val transactionMap = mutableMapOf<String, Pair<String?, MutableList<StatementContext>>>()
    private val closedTransactions = mutableSetOf<String>()
    private var transactionCount: Int = 0

    private val mutableTransactionLog = MutableLog<Unit>(
        name = "DatabaseTransactions",
        scope = GlobalScope,
        writeContentToLogFile = false,
    )
    private val mutableStatementLog = MutableLog<Unit>(
        name = "DatabaseStatements",
        scope = GlobalScope,
        writeToLogFile = false,
    )

    val transactionLog: Log<Unit> = mutableTransactionLog.asLog()
    val statementLog: Log<Unit> = mutableStatementLog.asLog()

    override fun onTransactionStart(transaction: Transaction, name: String?) {
        transaction.registerInterceptor(this)
        transactionMap[transaction.id] = Pair(name, mutableListOf())
    }

    override fun beforeCommit(transaction: Transaction) {
        closedTransactions.add(transaction.id)
        val transactionNumber = ++transactionCount
        transactionMap.remove(transaction.id)?.let { (transactionName, statements) ->
            mutableTransactionLog.info(
                title = "Transaction #$transactionNumber : ${transactionName ?: transaction.id} (${statements.size})",
                content = statements.joinToString(separator = "\n\n") { it.expandArgs(transaction) },
                duration = transaction.duration.milliseconds,
            )
        }
    }

    override fun log(context: StatementContext, transaction: Transaction) {
        require(transaction.id !in closedTransactions) { "transaction ${transaction.id} was already closed" }

        val data = transactionMap[transaction.id]
        val transactionName = data?.first
        val statements = data?.second

        statements?.add(context)

        val tables = context.statement.targets.joinToString { it.tableName }
        val transactionTitle = transactionName ?: transaction.id

        mutableStatementLog.info(
            title = "Statement #${transaction.statementCount} in $transactionTitle : ${context.statement.type} $tables",
            content = context.expandArgs(transaction),
            duration = transaction.duration.milliseconds,
        )
    }
}
