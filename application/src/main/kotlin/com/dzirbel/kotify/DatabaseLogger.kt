package com.dzirbel.kotify

import com.dzirbel.kotify.db.DB
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.log.Log
import com.dzirbel.kotify.log.MutableLog
import com.dzirbel.kotify.log.asLog
import com.dzirbel.kotify.log.info
import com.dzirbel.kotify.log.warn
import kotlinx.coroutines.GlobalScope
import org.jetbrains.exposed.sql.SqlLogger
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.name
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.statements.StatementInterceptor
import org.jetbrains.exposed.sql.statements.expandArgs
import kotlin.time.Duration.Companion.milliseconds

object DatabaseLogger : KotifyDatabase.TransactionListener, SqlLogger, StatementInterceptor {

    // map from ongoing transaction id to (name, statements)
    private val transactionMap = mutableMapOf<String, Pair<String?, MutableList<StatementContext>>>()

    // set of transaction ids that have been committed
    private val closedTransactions = mutableSetOf<String>()

    // map from database name to number of transactions committed in that database
    private val transactionCountByDatabaseName = mutableMapOf<String, Int>()

    data class LogData(val db: DB?) {
        constructor(transaction: Transaction) : this(DB.ofDatabaseName(transaction.db.name))
    }

    private val mutableTransactionLog = MutableLog<LogData>(
        name = "DatabaseTransactions",
        scope = GlobalScope,
        writeContentToLogFile = false,
    )

    private val mutableStatementLog = MutableLog<LogData>(
        name = "DatabaseStatements",
        scope = GlobalScope,
        writeToLogFile = false,
    )

    val transactionLog: Log<LogData> = mutableTransactionLog.asLog()
    val statementLog: Log<LogData> = mutableStatementLog.asLog()

    override fun onTransactionStart(transaction: Transaction, name: String?) {
        transaction.registerInterceptor(this)
        transactionMap[transaction.id] = Pair(name, mutableListOf())
    }

    override fun beforeCommit(transaction: Transaction) {
        closedTransactions.add(transaction.id)
        val dbName = transaction.db.name
        val transactionNumber = transactionCountByDatabaseName.compute(dbName) { _, count -> (count ?: 0) + 1 }
        transactionMap.remove(transaction.id)?.let { (transactionName, statements) ->
            mutableTransactionLog.info(
                title = "$dbName #$transactionNumber : ${transactionName ?: transaction.id} (${statements.size})",
                content = statements.joinToString(separator = "\n\n") { it.expandArgs(transaction) },
                duration = transaction.duration.milliseconds,
                data = LogData(transaction),
            )
        }
    }

    override fun log(context: StatementContext, transaction: Transaction) {
        require(transaction.id !in closedTransactions) { "transaction ${transaction.id} was already closed" }

        val data = transactionMap[transaction.id]

        if (data == null) {
            mutableTransactionLog.warn("No transaction data for ${transaction.id}", data = LogData(transaction))
            mutableStatementLog.warn("No transaction data for ${transaction.id}", data = LogData(transaction))
        }

        val transactionName = data?.first
        val statements = data?.second

        statements?.add(context)

        val tables = context.statement.targets.joinToString { it.tableName }
        val transactionTitle = transactionName ?: transaction.id
        val dbName = transaction.db.name

        mutableStatementLog.info(
            title = "$dbName statement #${transaction.statementCount} in $transactionTitle : " +
                "${context.statement.type} $tables",
            content = context.expandArgs(transaction),
            data = LogData(transaction),
        )
    }
}
