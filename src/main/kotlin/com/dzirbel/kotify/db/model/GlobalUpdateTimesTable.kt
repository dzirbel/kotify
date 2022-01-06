package com.dzirbel.kotify.db.model

import com.dzirbel.kotify.db.KotifyDatabase
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.time.Instant

/**
 * Stores the times at which global values were last updated.
 *
 * For example, sometimes we fetch the set of all saved artists from the Spotify API. We want to save the last time this
 * was done, but it doesn't fit cleanly into any other table. For lack of a better approach, we instead store it in this
 * key-value based table with a specific key.
 */
object GlobalUpdateTimesTable : Table(name = "global_update_times") {
    private val key: Column<String> = varchar(name = "key", length = 20).uniqueIndex()
    private val updateTime: Column<Instant> = timestamp(name = "update_time").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(key)

    /**
     * Gets the last time the given [key] was updated, or null if it has never been updated.
     */
    suspend fun updated(key: String): Instant? {
        return KotifyDatabase.transaction {
            select { GlobalUpdateTimesTable.key eq key }.firstOrNull()?.get(updateTime)
        }
    }

    /**
     * Sets the last time the given [key] was updated to [updateTime]. Creates the key-value pair if it does not exist,
     * or updates one if it does.
     *
     * Must be called from within a transaction.
     */
    fun setUpdated(key: String, updateTime: Instant = Instant.now()) {
        if (select { GlobalUpdateTimesTable.key eq key }.any()) {
            update(where = { GlobalUpdateTimesTable.key eq key }) {
                it[GlobalUpdateTimesTable.updateTime] = updateTime
            }
        } else {
            insert {
                it[GlobalUpdateTimesTable.key] = key
                it[GlobalUpdateTimesTable.updateTime] = updateTime
            }
        }
    }

    /**
     * Invalidates the last update time for the given [key].
     */
    suspend fun invalidate(key: String) {
        KotifyDatabase.transaction {
            deleteWhere { GlobalUpdateTimesTable.key eq key }
        }
    }
}
