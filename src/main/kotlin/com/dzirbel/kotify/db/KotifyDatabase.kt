package com.dzirbel.kotify.db

import com.dzirbel.kotify.Application
import com.dzirbel.kotify.Logger
import com.dzirbel.kotify.db.model.AlbumTable
import com.dzirbel.kotify.db.model.ArtistAlbumTable
import com.dzirbel.kotify.db.model.ArtistTable
import com.dzirbel.kotify.db.model.GenreTable
import com.dzirbel.kotify.db.model.GlobalUpdateTimesTable
import com.dzirbel.kotify.db.model.ImageTable
import com.dzirbel.kotify.db.model.PlaylistTable
import com.dzirbel.kotify.db.model.PlaylistTrackTable
import com.dzirbel.kotify.db.model.SavedAlbumRepository
import com.dzirbel.kotify.db.model.SavedArtistRepository
import com.dzirbel.kotify.db.model.SavedPlaylistRepository
import com.dzirbel.kotify.db.model.SavedTrackRepository
import com.dzirbel.kotify.db.model.TrackRatingTable
import com.dzirbel.kotify.db.model.TrackTable
import com.dzirbel.kotify.db.model.UserRepository
import com.dzirbel.kotify.db.model.UserTable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import java.io.File
import java.sql.Connection

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
    val dbFile: File by lazy {
        Application.cacheDir.resolve("cache.db")
    }

    val db: Database by lazy {
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_READ_UNCOMMITTED

        Database.connect(
            url = "jdbc:sqlite:${dbFile.absolutePath}",
            driver = "org.sqlite.JDBC",
            databaseConfig = DatabaseConfig {
                sqlLogger = Logger.Database
            },
        ).also {
            transaction(it) {
                @Suppress("SpreadOperator")
                SchemaUtils.createMissingTablesAndColumns(*tables)

                UserRepository.currentUserId.loadToCache()
            }
        }
    }

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
            transaction(db = db) {
                Logger.Database.registerTransaction(transaction = this, name = name)
                runBlocking { statement() }
            }
        } else {
            newSuspendedTransaction(context = dbDispatcher, db = db) {
                Logger.Database.registerTransaction(transaction = this, name = name)
                statement()
            }
        }
    }

    /**
     * Deletes all rows from the database, for use cleaning up after tests.
     */
    fun clear() {
        transaction(db) {
            tables.forEach { it.deleteAll() }
        }
    }

    /**
     * Invalidates the saved state of all savable entities, typically for use when the user logs out.
     */
    suspend fun clearSaved() {
        SavedArtistRepository.invalidateAll()
        SavedAlbumRepository.invalidateAll()
        SavedTrackRepository.invalidateAll()
        SavedPlaylistRepository.invalidateAll()
    }
}
