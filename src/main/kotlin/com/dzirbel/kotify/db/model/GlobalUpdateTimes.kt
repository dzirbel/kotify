package com.dzirbel.kotify.db.model

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.time.Instant

object GlobalUpdateTimesTable : Table(name = "global_update_times") {
    val key: Column<String> = varchar(name = "key", length = 20).uniqueIndex()
    val updateTime: Column<Instant> = timestamp(name = "update_time").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(key)
}

/**
 * Stores the times at which global values were last updated.
 *
 * For example, sometimes we fetch the set of all saved artists from the Spotify API. We want to save the last time this
 * was done, but it doesn't fit cleanly into any other table. For lack of a better approach, we instead store it in this
 * key-value based table with a specific key.
 */
object GlobalUpdateTimesRepository {
    /**
     * Determines whether the given [key] has ever been updated.
     *
     * Must be called from within a transaction.
     */
    fun hasBeenUpdated(key: String): Boolean {
        return GlobalUpdateTimesTable
            .select { GlobalUpdateTimesTable.key eq key }
            .any()
    }

    /**
     * Gets the last time the given [key] was updated, or null if it has never been updated.
     *
     * Must be called from within a transaction.
     */
    fun updated(key: String): Instant? {
        return GlobalUpdateTimesTable
            .select { GlobalUpdateTimesTable.key eq key }
            .firstOrNull()
            ?.get(GlobalUpdateTimesTable.updateTime)
    }

    /**
     * Sets the last time the given [key] was updated to [updateTime]. Creates the key-value pair if it does not exist,
     * or updates one if it does.
     *
     * Must be called from within a transaction.
     */
    fun setUpdated(key: String, updateTime: Instant = Instant.now()) {
        if (GlobalUpdateTimesTable.select { GlobalUpdateTimesTable.key eq key }.any()) {
            GlobalUpdateTimesTable.update(where = { GlobalUpdateTimesTable.key eq key }) {
                it[GlobalUpdateTimesTable.updateTime] = updateTime
            }
        } else {
            GlobalUpdateTimesTable.insert { statement ->
                statement[GlobalUpdateTimesTable.key] = key
                statement[GlobalUpdateTimesTable.updateTime] = updateTime
            }
        }
    }

    /**
     * Invalidates the last update time for the given [key].
     *
     * Must be called from within a transaction.
     */
    fun invalidate(key: String) {
        GlobalUpdateTimesTable.deleteWhere { GlobalUpdateTimesTable.key eq key }
    }
}
