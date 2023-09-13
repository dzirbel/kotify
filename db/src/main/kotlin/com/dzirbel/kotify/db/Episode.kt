package com.dzirbel.kotify.db

import com.dzirbel.kotify.db.model.Image
import com.dzirbel.kotify.db.model.ImageTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.Table

object EpisodeTable : SpotifyEntityTable(entityName = "episode") {
    val durationMs: Column<Long> = long("duration_ms")
    val description: Column<String> = text("description")
    val releaseDate: Column<String?> = text("release_date").nullable()
    val releaseDatePrecision: Column<String?> = text("release_date_precision").nullable()

    object EpisodeImageTable : Table() {
        val episode = reference("episode", EpisodeTable)
        val image = reference("image", ImageTable)
        override val primaryKey = PrimaryKey(episode, image)
    }
}

class Episode(id: EntityID<String>) : SpotifyEntity(id = id, table = EpisodeTable) {
    var durationMs: Long by EpisodeTable.durationMs
    var description: String by EpisodeTable.description
    var releaseDate: String? by EpisodeTable.releaseDate
    var releaseDatePrecision: String? by EpisodeTable.releaseDatePrecision

    var images: SizedIterable<Image> by Image via EpisodeTable.EpisodeImageTable

    companion object : SpotifyEntityClass<Episode>(EpisodeTable)
}
