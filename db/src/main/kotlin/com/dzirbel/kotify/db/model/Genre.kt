package com.dzirbel.kotify.db.model

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import java.util.UUID

object GenreTable : UUIDTable(name = "genres") {
    val name: Column<String> = text("genre").uniqueIndex()
}

class Genre(id: EntityID<UUID>) : UUIDEntity(id = id) {
    var name: String by GenreTable.name

    companion object : UUIDEntityClass<Genre>(GenreTable) {
        fun findOrCreate(genre: String): Genre {
            Genre.find { GenreTable.name eq genre }
                .limit(1)
                .firstOrNull()
                ?.let { return it }

            return Genre.new(id = UUID.randomUUID()) {
                name = genre
            }
        }
    }
}
