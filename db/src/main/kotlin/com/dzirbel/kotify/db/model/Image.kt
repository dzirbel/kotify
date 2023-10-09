package com.dzirbel.kotify.db.model

import com.dzirbel.kotify.db.util.single
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.times
import org.jetbrains.exposed.sql.and
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
        joinColumn.table.innerJoin(ImageTable)
            .single(column = url, order = width * height to SortOrder.ASC) {
                (joinColumn eq id) and (width greaterEq size.width) and (height greaterEq size.height)
            }
            ?.let { return it }

        // if no image is larger than the given dimensions, return the largest image
        // TODO do this in a single SQL query?
        return joinColumn.table.innerJoin(ImageTable)
            .single(column = url, order = width * height to SortOrder.DESC) { joinColumn eq id }
    }
}

class Image(id: EntityID<UUID>) : UUIDEntity(id = id) {
    var url: String by ImageTable.url
    var width: Int? by ImageTable.width
    var height: Int? by ImageTable.height

    companion object : UUIDEntityClass<Image>(ImageTable) {
        fun findOrCreate(url: String, width: Int?, height: Int?): Image {
            return Image.single { ImageTable.url eq url }
                ?.also { image ->
                    if (width != null && width != image.width) {
                        image.width = width
                    }

                    if (height != null && height != image.height) {
                        image.height = height
                    }
                }
                ?: new {
                    this.url = url
                    this.width = width
                    this.height = height
                }
        }
    }
}
