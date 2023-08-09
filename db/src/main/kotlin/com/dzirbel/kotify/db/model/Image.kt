package com.dzirbel.kotify.db.model

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.times
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.select
import java.util.UUID

/**
 * A simple wrapper class around the [width] and [height] of an image, in pixels.
 */
data class ImageSize(val width: Int, val height: Int)

object ImageTable : UUIDTable(name = "images") {
    val url: Column<String> = text("url").uniqueIndex()
    val width: Column<Int?> = integer("width").nullable()
    val height: Column<Int?> = integer("height").nullable()

    /**
     * Retrieves the image URL for the image with the smallest dimensions larger than [size].
     *
     * This is done by joining [ImageTable] on [joinColumn], typically the column in a join table with a foreign key to
     * the image and a foreign key to the entity that owns the image, and selecting where the entity ID matches [id].
     */
    fun smallestLargerThan(joinColumn: Column<EntityID<String>>, id: String, size: ImageSize): String? {
        return joinColumn.table
            .innerJoin(ImageTable)
            .slice(url)
            .select { joinColumn eq id }
            .andWhere { (width greaterEq size.width) and (height greaterEq size.height) }
            .orderBy(width * height to SortOrder.ASC)
            .limit(1)
            .firstOrNull()
            ?.get(url)
    }
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
