package com.dzirbel.kotify.db.model

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import java.util.UUID

object ImageTable : UUIDTable(name = "images") {
    val url: Column<String> = text("url").uniqueIndex()
    val width: Column<Int?> = integer("width").nullable()
    val height: Column<Int?> = integer("height").nullable()
}

class Image(id: EntityID<UUID>) : UUIDEntity(id = id) {
    var url: String by ImageTable.url
    var width: Int? by ImageTable.width
    var height: Int? by ImageTable.height

    companion object : UUIDEntityClass<Image>(ImageTable) {
        fun findOrCreate(url: String, width: Int?, height: Int?): Image {
            Image.find { ImageTable.url eq url }
                .limit(1)
                .firstOrNull()
                ?.also { image ->
                    if (width != null && width != image.width) {
                        image.width = width
                    }

                    if (height != null && height != image.height) {
                        image.height = height
                    }
                }
                ?.let { return it }

            return new(id = UUID.randomUUID()) {
                this.url = url
                this.width = width
                this.height = height
            }
        }
    }
}
