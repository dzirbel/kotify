package com.dzirbel.kotify.db

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.time.Instant

/**
 * Common table schema for representing the saved/unsaved status in the user's library of an entity with a string ID.
 *
 * This way, when we fetch the entire set of saved objects we can retain them all, even if they are not each already
 * present in the database.
 */
abstract class SavedEntityTable(name: String = "") : StringIdTable(name = name) {
    private val saved: Column<Boolean> = bool("saved").default(true)

    /**
     * The time at which the entity was saved, as provided by the Spotify API, or null if it was not saved or its save
     * time is unknown.
     */
    private val savedTime: Column<Instant?> = timestamp("saved_time").nullable()

    /**
     * The last time the saved status was individually checked.
     *
     * For most object types, saved status is often checked globally (i.e. getting the set of all currently saved
     * objects); that time is stored in [com.dzirbel.kotify.db.model.GlobalUpdateTimesRepository]. The individual saved
     * status of an entity may also be checked, which is stored here. The global update time may be more recent than
     * this one, in which case the global one should be used.
     */
    private val savedCheckTime: Column<Instant?> = timestamp("saved_check_time").nullable()

    /**
     * Determines whether the entity with the given [entityId] is saved, returning null if its status is unknown.
     *
     * Must be called from within a transaction.
     */
    fun isSaved(entityId: String): Boolean? {
        return select { id eq entityId }.firstOrNull()?.get(saved)
    }

    /**
     * Returns the [Instant] at which the entity with the given [entityId] was saved, or null if has not been saved or
     * its save time is unknown.
     */
    fun savedTime(entityId: String): Instant? {
        return select { id eq entityId }.firstOrNull()?.get(savedTime)
    }

    /**
     * Retrieves the last time the save state of the entity with the given [entityId] was individually checked.
     *
     * Must be called from within a transaction.
     */
    fun savedCheckTime(entityId: String): Instant? {
        return select { id eq entityId }.firstOrNull()?.get(savedCheckTime)
    }

    /**
     * Stores the entity with the given [entityId] as the given [saved] state, having been saved at the given
     * [savedTime].
     *
     * Must be called from within a transaction.
     */
    fun setSaved(entityId: String, saved: Boolean, savedTime: Instant?, savedCheckTime: Instant = Instant.now()) {
        if (select { id eq entityId }.any()) {
            update(where = { id eq entityId }) {
                it[this.saved] = saved
                if (saved) {
                    // only update savedTime if we have a fresh value
                    savedTime?.let { savedTime -> it[this.savedTime] = savedTime }
                } else {
                    it[this.savedTime] = null
                }
                it[this.savedCheckTime] = savedCheckTime
            }
        } else {
            insert {
                it[id] = entityId
                it[this.saved] = saved
                it[this.savedTime] = savedTime
                it[this.savedCheckTime] = savedCheckTime
            }
        }
    }

    /**
     * Gets the set of entity IDs which are marked as having been saved.
     */
    fun savedEntityIds(): Set<String> {
        return select { saved eq true }.mapTo(mutableSetOf()) { it[id].value }
    }
}
