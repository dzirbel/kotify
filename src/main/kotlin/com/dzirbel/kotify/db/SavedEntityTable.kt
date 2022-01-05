package com.dzirbel.kotify.db

import com.dzirbel.kotify.db.model.GlobalUpdateTimesTable
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.select
import java.time.Instant
import kotlin.properties.ReadOnlyProperty

/**
 * Common table schema for representing the saved/unsaved status in the user's library of an entity with a string ID.
 *
 * This way, when we fetch the entire set of saved objects we can retain them all, even if they are not each already
 * present in the database.
 */
abstract class SavedEntityTable(name: String = "") : StringIdTable(name = name) {
    val saved: Column<Boolean> = bool("saved").default(true)

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

    /**
     * Determines whether the entity with the given [entityId] is saved, returning null if its status is unknown.
     *
     * Must be called from within a transaction.
     */
    fun isSaved(entityId: String): Boolean? {
        return select { id eq entityId }.firstOrNull()?.get(saved)
    }

    /**
     * Stores the entity with the given [entityId] as the given [saved] state, having been saved at the given
     * [savedTime].
     *
     * Must be called from within a transaction.
     */
    fun setSaved(entityId: String, saved: Boolean, savedTime: Instant?, savedCheckTime: Instant = Instant.now()) {
        select { id eq entityId }
            .firstOrNull()
            ?: insert {
                it[id] = entityId
                it[this.saved] = saved
                it[this.savedTime] = savedTime?.takeIf { saved }
                it[this.savedCheckTime] = savedCheckTime
            }
    }

    /**
     * Gets the set of entity IDs which are marked as having been saved.
     */
    fun savedEntityIds(): Set<String> {
        return select { saved eq true }.mapTo(mutableSetOf()) { it[id].value }
    }
}

/**
 * A [SpotifyEntity] which is able to be saved to the user's library. The save status is stored in [savedEntityTable],
 * but convenience DAO accessors are provided here.
 */
abstract class SavableSpotifyEntity(
    id: EntityID<String>,
    table: SpotifyEntityTable,
    private val savedEntityTable: SavedEntityTable,
    globalUpdateKey: String,
) : SpotifyEntity(id, table) {
    val isSaved: Boolean? by isSaved<SavableSpotifyEntity>().cachedOutsideTransaction()
    val savedTime: Instant? by savedTime<SavableSpotifyEntity>().cachedOutsideTransaction()
    val savedCheckTime: Instant? by savedCheckTime<SavableSpotifyEntity>(
        savedEntityTable = savedEntityTable,
        globalUpdateKey = globalUpdateKey,
    ).cachedOutsideTransaction()

    /**
     * A [ReadOnlyProperty] which reflects the saved status of a [SavableSpotifyEntity] based on its [savedEntityTable]
     * which stores its saved-time records.
     */
    private fun <T : SpotifyEntity> isSaved(): ReadOnlyProperty<T, Boolean?> {
        return ReadOnlyProperty { thisRef, _ ->
            savedEntityTable.select { savedEntityTable.id eq thisRef.id }
                .firstOrNull()
                ?.get(savedEntityTable.saved)
        }
    }

    /**
     * A [ReadOnlyProperty] which reflects the saved time of a [SavableSpotifyEntity] based on its [savedEntityTable]
     * which stores its saved-time records.
     */
    private fun <T : SpotifyEntity> savedTime(): ReadOnlyProperty<T, Instant?> {
        return ReadOnlyProperty { thisRef, _ ->
            savedEntityTable.select { savedEntityTable.id eq thisRef.id }
                .firstOrNull()
                ?.let { it[savedEntityTable.savedTime] }
        }
    }

    /**
     * A [ReadOnlyProperty] which reflects the last time the saved status of a [SavableSpotifyEntity] was checked based
     * on its [savedEntityTable] which stores its individual saved-time records and [globalUpdateKey] which is the
     * [GlobalUpdateTimesTable] key for global saved checks on this entity type.
     */
    private fun <T : SpotifyEntity> savedCheckTime(
        savedEntityTable: SavedEntityTable,
        globalUpdateKey: String,
    ): ReadOnlyProperty<T, Instant?> {
        return ReadOnlyProperty { thisRef, _ ->
            val entitySavedCheckTime = savedEntityTable.select { savedEntityTable.id eq thisRef.id }
                .firstOrNull()
                ?.let { it[savedEntityTable.savedCheckTime] }

            val globalSavedCheckTime = runBlocking { GlobalUpdateTimesTable.updated(key = globalUpdateKey) }

            listOfNotNull(entitySavedCheckTime, globalSavedCheckTime).maxOrNull()
        }
    }

    /**
     * Stores this entity as the given [saved] state, having been saved at the given [saveTime].
     *
     * Must be called from within a transaction.
     */
    fun setSaved(saved: Boolean, saveTime: Instant = Instant.now(), savedCheckTime: Instant = Instant.now()) {
        savedEntityTable.setSaved(
            entityId = id.value,
            saved = saved,
            savedTime = saveTime,
            savedCheckTime = savedCheckTime,
        )
    }
}
