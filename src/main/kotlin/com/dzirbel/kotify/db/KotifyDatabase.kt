package com.dzirbel.kotify.db

import com.dzirbel.kotify.Application
import com.dzirbel.kotify.db.model.AlbumTable
import com.dzirbel.kotify.db.model.ArtistTable
import com.dzirbel.kotify.db.model.GenreTable
import com.dzirbel.kotify.db.model.ImageTable
import com.dzirbel.kotify.db.model.TrackTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.sql.Connection

private val tables = arrayOf(
    AlbumTable,
    AlbumTable.AlbumArtistTable,
    AlbumTable.AlbumGenreTable,
    AlbumTable.AlbumImageTable,
    AlbumTable.AlbumTrackTable,
    ArtistTable,
    ArtistTable.ArtistGenreTable,
    ArtistTable.ArtistImageTable,
    GenreTable,
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
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

        Database.connect("jdbc:sqlite:${dbFile.absolutePath}", driver = "org.sqlite.JDBC").also {
            transaction(it) {
                @Suppress("SpreadOperator")
                SchemaUtils.createMissingTablesAndColumns(*tables)
            }
        }
    }

    fun clear() {
        transaction(db) {
            tables.forEach { it.deleteAll() }
        }
    }
}
