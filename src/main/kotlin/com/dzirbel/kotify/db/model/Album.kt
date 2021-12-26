package com.dzirbel.kotify.db.model

import com.dzirbel.kotify.db.Repository
import com.dzirbel.kotify.db.SpotifyEntity
import com.dzirbel.kotify.db.SpotifyEntityClass
import com.dzirbel.kotify.db.SpotifyEntityTable
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.FullSpotifyAlbum
import com.dzirbel.kotify.network.model.SpotifyAlbum
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.Table
import java.time.Instant

object AlbumTable : SpotifyEntityTable(name = "albums") {
    val albumType: Column<SpotifyAlbum.Type?> = enumeration("album_type", SpotifyAlbum.Type::class).nullable()
    val releaseDate: Column<String?> = text("release_date").nullable()
    val releaseDatePrecision: Column<String?> = text("release_date_precision").nullable()
    val totalTracks: Column<UInt?> = uinteger("total_tracks").nullable()
    val label: Column<String?> = text("label").nullable()
    val popularity: Column<UInt?> = uinteger("popularity").nullable()

    object AlbumArtistTable : Table() {
        val album = reference("album", AlbumTable)
        val artist = reference("artist", ArtistTable)
        override val primaryKey = PrimaryKey(album, artist)
    }

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
}

class Album(id: EntityID<String>) : SpotifyEntity(id = id, table = AlbumTable) {
    var albumType: SpotifyAlbum.Type? by AlbumTable.albumType
    var releaseDate: String? by AlbumTable.releaseDate
    var releaseDatePrecision: String? by AlbumTable.releaseDatePrecision
    var totalTracks: UInt? by AlbumTable.totalTracks
    var label: String? by AlbumTable.label
    var popularity: UInt? by AlbumTable.popularity

    var artists: SizedIterable<Artist> by Artist via AlbumTable.AlbumArtistTable
    var images: SizedIterable<Image> by Image via AlbumTable.AlbumImageTable
    var genres: SizedIterable<Genre> by Genre via AlbumTable.AlbumGenreTable
    var tracks: SizedIterable<Track> by Track via AlbumTable.AlbumTrackTable

    /**
     * Whether all the tracks on this album (or the expected number of tracks) are in the database. Must be called from
     * within a transaction.
     */
    val hasAllTracks: Boolean
        get() = totalTracks?.let { tracks.count().toUInt() == it } == true

    companion object : SpotifyEntityClass<Album, SpotifyAlbum>(AlbumTable) {
        override fun Album.update(networkModel: SpotifyAlbum) {
            albumType = networkModel.albumType
            releaseDate = networkModel.releaseDate
            releaseDatePrecision = networkModel.releaseDatePrecision
            totalTracks = networkModel.totalTracks?.toUInt()

            artists = SizedCollection(networkModel.artists.mapNotNull { Artist.from(it) })
            images = SizedCollection(networkModel.images.map { Image.from(it) })

            if (networkModel is FullSpotifyAlbum) {
                fullUpdatedTime = Instant.now()

                label = networkModel.label
                popularity = networkModel.popularity.toUInt()
                totalTracks = networkModel.tracks.total.toUInt()

                genres = SizedCollection(networkModel.genres.map { Genre.from(it) })
                tracks = SizedCollection(networkModel.tracks.items.mapNotNull { Track.from(it) })
            }
        }
    }
}

object AlbumRepository : Repository<Album, SpotifyAlbum>(Album) {
    override suspend fun fetch(id: String) = Spotify.Albums.getAlbum(id = id)
    override suspend fun fetch(ids: List<String>) = Spotify.Albums.getAlbums(ids = ids)
}
