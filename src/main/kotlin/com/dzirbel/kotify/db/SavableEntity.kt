package com.dzirbel.kotify.db

import com.dzirbel.kotify.db.model.GlobalUpdateTimesTable
import org.jetbrains.exposed.sql.select
import java.time.Instant
import kotlin.properties.ReadOnlyProperty

/**
 * Returns a [ReadOnlyProperty] which reflects the saved status of a [SpotifyEntity] based on the given
 * [savedEntityTable] which stores its saved-time records.
 */
fun <T : SpotifyEntity> isSaved(savedEntityTable: SavedEntityTable): ReadOnlyProperty<T, Boolean?> {
    return ReadOnlyProperty { thisRef, _ ->
        savedEntityTable.select { savedEntityTable.id eq thisRef.id }
            .firstOrNull()
            ?.let { it[savedEntityTable.savedTime] != null }
    }
}

/**
 * Returns a [ReadOnlyProperty] which reflects the saved time of a [SpotifyEntity] based on the given
 * [savedEntityTable] which stores its saved-time records.
 */
fun <T : SpotifyEntity> savedTime(savedEntityTable: SavedEntityTable): ReadOnlyProperty<T, Instant?> {
    return ReadOnlyProperty { thisRef, _ ->
        savedEntityTable.select { savedEntityTable.id eq thisRef.id }
            .firstOrNull()
            ?.let { it[savedEntityTable.savedTime] }
    }
}

/**
 * Returns a [ReadOnlyProperty] which reflects the last time the saved status of a [SpotifyEntity] was checked based on
 * the given [savedEntityTable] which stores its individual saved-time records and [globalUpdateKey] which is the
 * [GlobalUpdateTimesTable] key for global saved checks on this entity type.
 */
fun <T : SpotifyEntity> savedCheckTime(
    savedEntityTable: SavedEntityTable,
    globalUpdateKey: String,
): ReadOnlyProperty<T, Instant?> {
    return ReadOnlyProperty { thisRef, _ ->
        val entitySavedCheckTime = savedEntityTable.select { savedEntityTable.id eq thisRef.id }
            .firstOrNull()
            ?.let { it[savedEntityTable.savedCheckTime] }

        val globalSavedCheckTime = GlobalUpdateTimesTable.updated(key = globalUpdateKey)

        listOfNotNull(entitySavedCheckTime, globalSavedCheckTime).maxOrNull()
    }
}
