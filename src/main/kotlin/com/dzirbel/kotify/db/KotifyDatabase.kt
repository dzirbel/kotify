package com.dzirbel.kotify.db

import com.dzirbel.kotify.Application
import com.dzirbel.kotify.Logger
import com.dzirbel.kotify.db.model.AlbumTable
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
import com.dzirbel.kotify.db.model.UserTable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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
            }
        }
    }

    /**
     * A [CoroutineDispatcher] which is used to execute database transactions, in particular limiting them to run
     * serially, i.e. with no parallelism.
     */
    private val dbDispatcher: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(1)

    /**
     * Creates a transaction on this [db] with appropriate logic; in particular, using the [dbDispatcher] which operates
     * on a single thread to avoid database locking.
     */
    suspend fun <T> transaction(statement: suspend Transaction.() -> T): T {
        check(db.transactionManager.currentOrNull() == null) { "transaction already in progress" }

        return newSuspendedTransaction(
            context = dbDispatcher,
            db = db,
            statement = {
                Logger.Database.registerTransaction(this)
                statement()
            },
        )
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
