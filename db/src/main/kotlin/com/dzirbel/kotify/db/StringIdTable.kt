package com.dzirbel.kotify.db

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

/**
 * A simple [IdTable] which uses a [String] as its [id] and [primaryKey].
 */
abstract class StringIdTable(name: String = "", idLength: Int = STRING_ID_LENGTH) : IdTable<String>(name) {
    // Spotify ID's typically have 22 characters, base64 encoded
    final override val id: Column<EntityID<String>> = varchar("id", length = idLength).entityId()
    final override val primaryKey = PrimaryKey(id)

    companion object {
        /**
         * The default length for the ID column; Spotify ID's typically have 22 characters (base62-encoded).
         *
         * See https://developer.spotify.com/documentation/web-api/#spotify-uris-and-ids
         */
        const val STRING_ID_LENGTH = 32
    }
}
