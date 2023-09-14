package com.dzirbel.kotify.db

import com.dzirbel.kotify.db.model.AlbumTable
import com.dzirbel.kotify.db.model.ArtistAlbumTable
import com.dzirbel.kotify.db.model.ArtistTable
import com.dzirbel.kotify.db.model.GenreTable
import com.dzirbel.kotify.db.model.GlobalUpdateTimesTable
import com.dzirbel.kotify.db.model.ImageTable
import com.dzirbel.kotify.db.model.PlaylistTable
import com.dzirbel.kotify.db.model.PlaylistTrackTable
import com.dzirbel.kotify.db.model.TrackRatingTable
import com.dzirbel.kotify.db.model.TrackTable
import com.dzirbel.kotify.db.model.UserTable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlLogger
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import java.io.File
import java.sql.Connection
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

enum class DB(vararg val tables: Table) {
    CACHE(
        AlbumTable,
        AlbumTable.AlbumGenreTable,
        AlbumTable.AlbumImageTable,
        AlbumTable.AlbumTrackTable,
        AlbumTable.SavedAlbumsTable,
        ArtistTable,
        ArtistTable.ArtistGenreTable,
        ArtistTable.ArtistImageTable,
        ArtistTable.SavedArtistsTable,
        ArtistAlbumTable,
        GenreTable,
        GlobalUpdateTimesTable,
        ImageTable,
        PlaylistTable,
        PlaylistTable.PlaylistImageTable,
        PlaylistTable.SavedPlaylistsTable,
        PlaylistTrackTable,
        TrackTable,
        TrackTable.TrackArtistTable,
        TrackTable.SavedTracksTable,
        UserTable,
        UserTable.CurrentUserTable,
        UserTable.UserImageTable,
    ),
    RATINGS(
        TrackRatingTable,
    ),
    ;

    val databaseName = "${name.lowercase(Locale.US)}.db"

    companion object {
        fun ofDatabaseName(databaseName: String): DB? {
            return entries.find { it.databaseName == databaseName }
        }
    }
}

/**
 * Global wrapper on database connections.
 */
object KotifyDatabase {
    interface TransactionListener {
        fun onTransactionStart(transaction: Transaction, name: String?)
    }

    interface DatabaseContext {
        suspend fun <T> transaction(name: String?, statement: Transaction.() -> T): T
    }

    private lateinit var databaseByDB: Array<Database>

    /**
     * A [CoroutineDispatcher] which is used to execute database transactions, in particular limiting them to run
     * serially, i.e. with no parallelism.
     */
    private val dbDispatcherByDB = Array(DB.entries.size) { Dispatchers.IO.limitedParallelism(1) }

    private var synchronousTransactions = false

    private val initialized = AtomicBoolean(false)

    private val transactionListeners = mutableListOf<TransactionListener>()

    /**
     * Global delay applied to each transaction to simulate slower database conditions during performance testing.
     */
    var transactionDelayMs: Long = 0L

    /**
     * Whether calls to [KotifyDatabase] are allowed; defaults to false to prevent access from tests which should not
     * make accidental (and potentially expensive) calls to the database.
     */
    var enabled: Boolean = false

    fun init(dbDir: File, sqlLogger: SqlLogger = NoOpSqlLogger, deleteOnExit: Boolean = false) {
        check(!initialized.getAndSet(true)) { "already initialized" }

        val databaseConfig = DatabaseConfig {
            this.sqlLogger = sqlLogger

            // do not retry on SQL errors, since transient failures to a local SQLite database are unexpected
            defaultRepetitionAttempts = 0

            // no need for transaction isolation since transactions are run with limited parallelism
            defaultIsolationLevel = Connection.TRANSACTION_READ_UNCOMMITTED

            // TODO disable this again once https://youtrack.jetbrains.com/issue/EXPOSED-169 is resolved
            // do not keep entities in database cache; they are stored by repositories
            maxEntitiesToStoreInCachePerEntity = Int.MAX_VALUE
        }

        databaseByDB = Array(DB.entries.size) { i ->
            val db = DB.entries[i]
            val dbFile = dbDir.resolve(db.databaseName)
            if (deleteOnExit) dbFile.deleteOnExit()
            connectToSQLiteDatabase(databaseFile = dbFile, databaseConfig = databaseConfig)
                .also { database ->
                    transaction(database) {
                        // TODO create tables and columns via migrations (with tests verifying schema)
                        @Suppress("SpreadOperator")
                        SchemaUtils.createMissingTablesAndColumns(*db.tables)
                    }
                }
        }
    }

    fun addTransactionListener(listener: TransactionListener) {
        transactionListeners.add(listener)
    }

    /**
     * Toggles running [transaction] synchronously (rather than via [newSuspendedTransaction]) inside [block].
     *
     * This is intended for use in tests where a test scheduler is used to advance time; this will cause the test
     * scheduler to proceed as soon as a suspended transaction is begun. Running synchronously avoids the test scheduler
     * advancing until the transaction is complete.
     */
    fun <T> withSynchronousTransactions(block: () -> T): T {
        synchronousTransactions = true
        val result = block()
        synchronousTransactions = false
        return result
    }

    operator fun get(db: DB): DatabaseContext {
        return DatabaseContextImpl(databaseByDB[db.ordinal], dbDispatcherByDB[db.ordinal])
    }

    /**
     * Deletes all rows from the database, for use cleaning up after tests.
     */
    fun deleteAll() {
        for (db in DB.entries) {
            transaction(databaseByDB[db.ordinal]) {
                for (table in db.tables) table.deleteAll()
            }
        }
    }

    private fun connectToSQLiteDatabase(databaseFile: File, databaseConfig: DatabaseConfig): Database {
        return Database.connect(
            url = "jdbc:sqlite:${databaseFile.absolutePath}",
            driver = "org.sqlite.JDBC",
            databaseConfig = databaseConfig,
        )
    }

    private class DatabaseContextImpl(
        private val db: Database,
        private val context: CoroutineContext,
    ) : DatabaseContext {
        override suspend fun <T> transaction(name: String?, statement: Transaction.() -> T): T {
            check(enabled)
            check(initialized.get()) { "database not initialized" }
            check(db.transactionManager.currentOrNull() == null) { "transaction already in progress" }

            delay(transactionDelayMs)

            return if (synchronousTransactions) {
                synchronized(this) {
                    transaction(db = db) {
                        transactionListeners.forEach {
                            it.onTransactionStart(transaction = this, name = name)
                        }

                        statement()
                    }
                }
            } else {
                withContext(context) {
                    transaction(db = db) {
                        transactionListeners.forEach {
                            it.onTransactionStart(transaction = this, name = name)
                        }

                        statement()
                    }
                }
            }
        }
    }

    private object NoOpSqlLogger : SqlLogger {
        override fun log(context: StatementContext, transaction: Transaction) {
            // no-op
        }
    }
}
