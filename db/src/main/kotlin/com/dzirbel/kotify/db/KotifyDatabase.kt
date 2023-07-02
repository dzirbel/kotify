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
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlLogger
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import java.io.File
import java.sql.Connection
import java.util.concurrent.atomic.AtomicBoolean

private val tables = arrayOf(
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
    TrackRatingTable,
    UserTable,
    UserTable.CurrentUserTable,
    UserTable.UserImageTable,
)

/**
 * Global wrapper on the database connection [KotifyDatabase.db].
 */
object KotifyDatabase {
    interface TransactionListener {
        fun onTransactionStart(transaction: Transaction, name: String?)
    }

    // TODO make private? (only used in tests)
    lateinit var db: Database
        private set

    /**
     * A [CoroutineDispatcher] which is used to execute database transactions, in particular limiting them to run
     * serially, i.e. with no parallelism.
     */
    private val dbDispatcher: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(1)

    private var synchronousTransactions = false

    /**
     * Global delay applied to each transaction to simulate slower database conditions during performance testing.
     */
    var transactionDelayMs: Long = 0L

    private val initialized = AtomicBoolean(false)

    private val transactionListeners = mutableListOf<TransactionListener>()

    fun init(dbFile: File, sqlLogger: SqlLogger = NoOpSqlLogger, onConnect: () -> Unit = {}) {
        check(!initialized.getAndSet(true)) { "already initialized" }

        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_READ_UNCOMMITTED

        db = Database.connect(
            url = "jdbc:sqlite:${dbFile.absolutePath}",
            driver = "org.sqlite.JDBC",
            databaseConfig = DatabaseConfig {
                this.sqlLogger = sqlLogger
            },
        )

        transaction(db) {
            // TODO create tables and columns via migrations (with tests verifying schema)
            @Suppress("SpreadOperator")
            SchemaUtils.createMissingTablesAndColumns(*tables)

            onConnect()
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
    fun withSynchronousTransactions(block: () -> Unit) {
        synchronousTransactions = true
        block()
        synchronousTransactions = false
    }

    /**
     * Creates a transaction on this [db] with appropriate logic; in particular, using the [dbDispatcher] which operates
     * on a single thread to avoid database locking.
     */
    suspend fun <T> transaction(name: String?, statement: suspend Transaction.() -> T): T {
        check(db.transactionManager.currentOrNull() == null) { "transaction already in progress" }

        if (transactionDelayMs > 0) {
            delay(transactionDelayMs)
        }

        return if (synchronousTransactions) {
            synchronized(this) {
                transaction(db = db) {
                    transactionListeners.forEach {
                        it.onTransactionStart(transaction = this, name = name)
                    }

                    runBlocking { statement() }
                }
            }
        } else {
            newSuspendedTransaction(context = dbDispatcher, db = db) {
                transactionListeners.forEach {
                    it.onTransactionStart(transaction = this, name = name)
                }

                statement()
            }
        }
    }

    /**
     * Deletes all rows from the database, for use cleaning up after tests.
     */
    fun deleteAll() {
        transaction(db) {
            tables.forEach { it.deleteAll() }
        }
    }

    private object NoOpSqlLogger : SqlLogger {
        override fun log(context: StatementContext, transaction: Transaction) {
            // no-op
        }
    }
}
