package com.dzirbel.kotify.repository.global

import com.dzirbel.kotify.db.model.GlobalUpdateTimesTable
import com.dzirbel.kotify.db.util.single
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.upsert
import java.time.Instant

/**
 * Stores the times at which global values were last updated.
 *
 * For example, sometimes we fetch the set of all saved artists from the Spotify API. We want to save the last time this
 * was done, but it doesn't fit cleanly into any other table. For lack of a better approach, we instead store it in this
 * key-value based table with a specific key.
 */
internal object GlobalUpdateTimesRepository {
    /**
     * Determines whether the given [key] has ever been updated.
     *
     * Must be called from within a transaction.
     */
    fun hasBeenUpdated(key: String): Boolean {
        return !GlobalUpdateTimesTable.selectAll()
            .where { GlobalUpdateTimesTable.key eq key }
            .empty()
    }

    /**
     * Gets the last time the given [key] was updated, or null if it has never been updated.
     *
     * Must be called from within a transaction.
     */
    fun updated(key: String): Instant? {
        return GlobalUpdateTimesTable.single(GlobalUpdateTimesTable.updateTime) { GlobalUpdateTimesTable.key eq key }
    }

    /**
     * Sets the last time the given [key] was updated to [updateTime]. Creates the key-value pair if it does not exist,
     * or updates one if it does.
     *
     * Must be called from within a transaction.
     */
    fun setUpdated(key: String, updateTime: Instant) {
        GlobalUpdateTimesTable.upsert { statement ->
            statement[GlobalUpdateTimesTable.key] = key
            statement[GlobalUpdateTimesTable.updateTime] = updateTime
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
