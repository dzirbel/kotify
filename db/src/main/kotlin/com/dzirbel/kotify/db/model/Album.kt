package com.dzirbel.kotify.db.model

import com.dzirbel.kotify.db.SavedEntityTable
import com.dzirbel.kotify.db.SpotifyEntity
import com.dzirbel.kotify.db.SpotifyEntityClass
import com.dzirbel.kotify.db.SpotifyEntityTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

enum class AlbumType(val displayName: String, val iconName: String) {
    ALBUM(displayName = "Album", iconName = "album"),
    EP(displayName = "EP", iconName = "album"),
    SINGLE(displayName = "Single", iconName = "audiotrack"),
    COMPILATION(displayName = "Compilation", iconName = "library-music"),
    APPEARS_ON(displayName = "Appears On", iconName = "audio-file"),
}

object AlbumTable : SpotifyEntityTable(entityName = "album") {
    val albumType: Column<AlbumType?> = enumeration("album_type", AlbumType::class).nullable()
    val releaseDate: Column<String?> = text("release_date").nullable()
    val releaseDatePrecision: Column<String?> = text("release_date_precision").nullable()
    val totalTracks: Column<Int?> = integer("total_tracks").nullable()
    val tracksFetched: Column<Instant?> = timestamp("tracks_fetched").nullable()
    val label: Column<String?> = text("label").nullable()
    val popularity: Column<Int?> = integer("popularity").nullable()

    object AlbumImageTable : Table() {
        val album = reference("album", AlbumTable)
        val image = reference("image", ImageTable)
        override val primaryKey = PrimaryKey(album, image)
    }

    object AlbumGenreTable : Table() {
        val album = reference("album", AlbumTable)
        val genre = reference("genre", GenreTable)
        override val primaryKey = PrimaryKey(album, genre)
    }

    object AlbumTrackTable : Table() {
        val album = reference("album", AlbumTable)
        val track = reference("track", TrackTable)
        override val primaryKey = PrimaryKey(album, track)
    }

    object SavedAlbumsTable : SavedEntityTable(name = "saved_albums")
}

class Album(id: EntityID<String>) : SpotifyEntity(id = id, table = AlbumTable) {
    var albumType: AlbumType? by AlbumTable.albumType
    var releaseDate: String? by AlbumTable.releaseDate
    var releaseDatePrecision: String? by AlbumTable.releaseDatePrecision
    var totalTracks: Int? by AlbumTable.totalTracks
    var tracksFetched: Instant? by AlbumTable.tracksFetched
    var label: String? by AlbumTable.label
    var popularity: Int? by AlbumTable.popularity

    var images: SizedIterable<Image> by Image via AlbumTable.AlbumImageTable
    var genres: SizedIterable<Genre> by Genre via AlbumTable.AlbumGenreTable
    var tracks: SizedIterable<Track> by Track via AlbumTable.AlbumTrackTable
    var artists: SizedIterable<Artist> by Artist via ArtistAlbumTable

    companion object : SpotifyEntityClass<Album>(AlbumTable)
}
