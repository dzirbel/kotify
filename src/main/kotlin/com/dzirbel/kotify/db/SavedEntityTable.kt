package com.dzirbel.kotify.db

import com.dzirbel.kotify.db.model.AlbumTable
import com.dzirbel.kotify.db.model.GlobalUpdateTimesTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.select
import java.time.Instant

/**
 * Common table schema for representing the saved/unsaved status of an entity with a string ID.
 *
 * This way, when we fetch the entire set of saved objects we can retain them all, even if they are not each already
 * present in the database.
 */
abstract class SavedEntityTable(name: String = "") : StringIdTable(name = name) {
    /**
     * The time at which the entity was saved, as provided by the Spotify API, or null if it was not saved.
     */
    val savedTime: Column<Instant?> = timestamp("saved_time").nullable()

    /**
     * The last time the saved status was individually checked.
     *
     * For most object types, saved status is often checked globally (i.e. getting the set of all currently saved
     * objects); that time is stored in [com.dzirbel.kotify.db.model.GlobalUpdateTimesTable]. The individual saved
     * status of an entity may also be checked, which is stored here. The global update time may be more recent than
     * this one, in which case the global one should be used.
     */
    val savedCheckTime: Column<Instant?> = timestamp("saved_check_time").nullable()
}

abstract class SavableSpotifyEntity(
    id: EntityID<String>,
    table: SpotifyEntityTable,
    private val savedEntityTable: SavedEntityTable,
) : SpotifyEntity(id, table) {
    val isSaved: Boolean? by isSaved<SavableSpotifyEntity>(savedEntityTable).cachedOutsideTransaction()
    val savedTime: Instant? by savedTime<SavableSpotifyEntity>(savedEntityTable).cachedOutsideTransaction()
    val savedCheckTime: Instant? by savedCheckTime<SavableSpotifyEntity>(
        savedEntityTable = AlbumTable.SavedAlbumsTable,
        globalUpdateKey = GlobalUpdateTimesTable.Keys.SAVED_ALBUMS,
    ).cachedOutsideTransaction()

    fun setSaved(saved: Boolean, saveTime: Instant = Instant.now()) {
        savedEntityTable.select { savedEntityTable.id eq id }
            .firstOrNull()
            ?: savedEntityTable.insert {
                it[savedTime] = if (saved) saveTime else null
                it[savedCheckTime] = saveTime
            }
    }
}
