package com.dzirbel.kotify.db.model

import com.dzirbel.kotify.network.model.SpotifyImage
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.times
import java.util.UUID

/**
 * Returns the [Image] with the largest width x height in this [SizedIterable], or null if it is empty.
 */
fun SizedIterable<Image>.largest(): Image? {
    return this
        .orderBy(ImageTable.width.times(ImageTable.height) to SortOrder.DESC)
        .limit(1)
        .firstOrNull()
}

/**
 * Returns the [Image] with the smallest width x height in this [SizedIterable], or null if it is empty.
 */
fun SizedIterable<Image>.smallest(): Image? {
    return this
        .orderBy(ImageTable.width.times(ImageTable.height) to SortOrder.ASC)
        .limit(1)
        .firstOrNull()
}

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
        fun from(networkModel: SpotifyImage): Image {
            Image.find { ImageTable.url eq networkModel.url }
                .limit(1)
                .firstOrNull()
                ?.let { return it }

            return new(id = UUID.randomUUID()) {
                url = networkModel.url
                width = networkModel.width
                height = networkModel.height
            }
        }
    }
}
