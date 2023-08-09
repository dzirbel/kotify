package com.dzirbel.kotify.db

import com.dzirbel.kotify.util.collections.zipLazy
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchUpsert
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.upsert
import java.time.Instant

/**
 * Common table schema for representing the saved/unsaved status in the user's library of an entity with a string ID.
 *
 * This way, when we fetch the entire set of saved objects we can retain them all, even if they are not each already
 * present in the database.
 */
abstract class SavedEntityTable(name: String) : Table(name = name) {
    private val entityIdColumn: Column<String> = varchar("id", length = StringIdTable.STRING_ID_LENGTH)
    private val userIdColumn: Column<String> = varchar("user_id", length = StringIdTable.STRING_ID_LENGTH)

    final override val primaryKey = PrimaryKey(entityIdColumn, userIdColumn)

    private val savedColumn: Column<Boolean> = bool("saved")

    /**
     * The time at which the entity was saved, as provided by the Spotify API, or null if it was not saved or its save
     * time is unknown.
     */
    private val savedTimeColumn: Column<Instant?> = timestamp("saved_time").nullable()

    /**
     * The last time the saved status was individually checked.
     *
     * For most object types, saved status is often checked globally (i.e. getting the set of all currently saved
     * objects); that time is stored in [com.dzirbel.kotify.repository.global.GlobalUpdateTimesRepository]. The
     * individual saved status of an entity may also be checked, which is stored here. The global update time may be
     * more recent than this one, in which case the global one should be used.
     */
    private val savedCheckTimeColumn: Column<Instant?> = timestamp("saved_check_time").nullable()

    /**
     * Determines whether the entity with the given [entityId] is saved, returning null if its status is unknown.
     *
     * Must be called from within a transaction.
     */
    fun isSaved(entityId: String, userId: String): Boolean? {
        return slice(savedColumn)
            .select { (entityIdColumn eq entityId) and (userIdColumn eq userId) }
            .firstOrNull()
            ?.get(savedColumn)
    }

    /**
     * Returns the [Instant] at which the entity with the given [entityId] was saved, or null if has not been saved or
     * its save time is unknown.
     */
    fun savedTime(entityId: String, userId: String): Instant? {
        return slice(savedTimeColumn)
            .select { (entityIdColumn eq entityId) and (userIdColumn eq userId) }
            .firstOrNull()
            ?.get(savedTimeColumn)
    }

    /**
     * Retrieves the last time the save state of the entity with the given [entityId] was individually checked.
     *
     * Must be called from within a transaction.
     */
    fun savedCheckTime(entityId: String, userId: String): Instant? {
        return slice(savedCheckTimeColumn)
            .select { (entityIdColumn eq entityId) and (userIdColumn eq userId) }
            .firstOrNull()
            ?.get(savedCheckTimeColumn)
    }

    /**
     * Stores the entity with the given [entityId] as the given [saved] state for the user with the given [userId],
     * having been saved at the given [savedTime] if known.
     *
     * Must be called from within a transaction.
     */
    fun setSaved(entityId: String, saved: Boolean, userId: String, savedTime: Instant?, savedCheckTime: Instant) {
        upsert(entityIdColumn, userIdColumn) { statement ->
            statement[entityIdColumn] = entityId
            statement[userIdColumn] = userId

            statement[savedColumn] = saved
            // TODO do not change savedTime (in particular, do not set it to null) if saved value has not changed
            statement[savedTimeColumn] = savedTime?.takeIf { saved }
            statement[savedCheckTimeColumn] = savedCheckTime
        }
    }

    /**
     * Stores the entities with the given [entityIds] as their respective [saved] states for the user with the given
     * [userId], having been saved at the given [savedTime] if known.
     *
     * Must be called from within a transaction.
     */
    fun setSaved(
        entityIds: Iterable<String>,
        saved: Iterable<Boolean>,
        userId: String,
        savedTime: Instant?,
        savedCheckTime: Instant,
    ) {
        batchUpsert(data = entityIds.zipLazy(saved), shouldReturnGeneratedValues = false) { (entityId, saved) ->
            this[entityIdColumn] = entityId
            this[userIdColumn] = userId

            this[savedColumn] = saved
            // TODO do not change savedTime (in particular, do not set it to null) if saved value has not changed
            this[savedTimeColumn] = savedTime?.takeIf { saved }
            this[savedCheckTimeColumn] = savedCheckTime
        }
    }

    /**
     * Gets the set of entity IDs which are marked as having been saved.
     */
    fun savedEntityIds(userId: String): Set<String> {
        return slice(entityIdColumn)
            .select { (savedColumn eq true) and (userIdColumn eq userId) }
            .mapTo(mutableSetOf()) { it[entityIdColumn] }
    }
}
