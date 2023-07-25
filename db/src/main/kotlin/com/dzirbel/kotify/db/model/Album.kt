package com.dzirbel.kotify.db.model

import com.dzirbel.kotify.db.ReadOnlyCachedProperty
import com.dzirbel.kotify.db.ReadWriteCachedProperty
import com.dzirbel.kotify.db.SavedEntityTable
import com.dzirbel.kotify.db.SpotifyEntity
import com.dzirbel.kotify.db.SpotifyEntityClass
import com.dzirbel.kotify.db.SpotifyEntityTable
import com.dzirbel.kotify.db.TransactionReadOnlyCachedProperty
import com.dzirbel.kotify.db.cachedAsList
import com.dzirbel.kotify.db.cachedReadOnly
import com.dzirbel.kotify.db.util.largest
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

enum class AlbumType(val displayName: String, val iconName: String) {
    ALBUM(displayName = "Album", iconName = "album"),
    SINGLE(displayName = "Single", iconName = "audiotrack"),
    COMPILATION(displayName = "Compilation", iconName = "library-music"),
    APPEARS_ON(displayName = "Appears On", iconName = "audio-file"),
}

object AlbumTable : SpotifyEntityTable(name = "albums") {
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

    val images: ReadWriteCachedProperty<List<Image>> by (Image via AlbumTable.AlbumImageTable).cachedAsList()
    val genres: ReadWriteCachedProperty<List<Genre>> by (Genre via AlbumTable.AlbumGenreTable).cachedAsList()
    val tracks: ReadWriteCachedProperty<List<Track>> by (Track via AlbumTable.AlbumTrackTable).cachedAsList()

    val artists: ReadOnlyCachedProperty<List<Artist>> = ReadOnlyCachedProperty {
        ArtistAlbum.find { ArtistAlbumTable.album eq id }.map { it.artist.live }
    }

    val parsedReleaseDate: ReleaseDate? by lazy {
        releaseDate?.let { ReleaseDate.parse(it) }
    }

    val largestImage: TransactionReadOnlyCachedProperty<Image?> by (Image via AlbumTable.AlbumImageTable)
        .cachedReadOnly(transactionName = "load album ${id.value} largest image") { it.largest() }

    companion object : SpotifyEntityClass<Album>(AlbumTable)
}
