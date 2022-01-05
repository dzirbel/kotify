package com.dzirbel.kotify.db

import com.dzirbel.kotify.Application
import com.dzirbel.kotify.db.model.AlbumTable
import com.dzirbel.kotify.db.model.ArtistTable
import com.dzirbel.kotify.db.model.GenreTable
import com.dzirbel.kotify.db.model.GlobalUpdateTimesTable
import com.dzirbel.kotify.db.model.ImageTable
import com.dzirbel.kotify.db.model.TrackTable
import kotlinx.coroutines.asCoroutineDispatcher
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.sql.Connection
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

private val tables = arrayOf(
    AlbumTable,
    AlbumTable.AlbumArtistTable,
    AlbumTable.AlbumGenreTable,
    AlbumTable.AlbumImageTable,
    AlbumTable.AlbumTrackTable,
    AlbumTable.SavedAlbumsTable,
    ArtistTable,
    ArtistTable.ArtistGenreTable,
    ArtistTable.ArtistImageTable,
    ArtistTable.SavedArtistsTable,
    GenreTable,
    GlobalUpdateTimesTable,
    ImageTable,
    TrackTable,
    TrackTable.TrackArtistTable,
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

        Database.connect("jdbc:sqlite:${dbFile.absolutePath}", driver = "org.sqlite.JDBC").also {
            transaction(it) {
                @Suppress("SpreadOperator")
                SchemaUtils.createMissingTablesAndColumns(*tables)
            }
        }
    }

    private var dbThread: Thread? = null
    private val dbThreadFactory = ThreadFactory { runnable ->
        check(dbThread == null) { "Multiple db threads created" }
        Thread(runnable).also { dbThread = it }
    }
    private val dbDispatcher = Executors.newSingleThreadExecutor(dbThreadFactory).asCoroutineDispatcher()

    /**
     * Creates a transaction on this [db] with appropriate logic; in particular, using the [dbDispatcher] which operates
     * on a single thread to avoid database locking.
     */
    suspend fun <T> transaction(statement: suspend Transaction.() -> T): T {
        return newSuspendedTransaction(context = dbDispatcher, db = db, statement = statement)
    }

    /**
     * Deletes all rows from the database, for use cleaning up after tests.
     */
    fun clear() {
        transaction(db) {
            tables.forEach { it.deleteAll() }
        }
    }
}
