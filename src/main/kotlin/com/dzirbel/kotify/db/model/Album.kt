package com.dzirbel.kotify.db.model

import com.dzirbel.kotify.db.DatabaseRepository
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.ReadOnlyCachedProperty
import com.dzirbel.kotify.db.ReadWriteCachedProperty
import com.dzirbel.kotify.db.SavedDatabaseRepository
import com.dzirbel.kotify.db.SavedEntityTable
import com.dzirbel.kotify.db.SpotifyEntity
import com.dzirbel.kotify.db.SpotifyEntityClass
import com.dzirbel.kotify.db.SpotifyEntityTable
import com.dzirbel.kotify.db.cachedAsList
import com.dzirbel.kotify.db.cachedReadOnly
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.FullSpotifyAlbum
import com.dzirbel.kotify.network.model.ReleaseDate
import com.dzirbel.kotify.network.model.SimplifiedSpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyAlbum
import com.dzirbel.kotify.network.model.SpotifySavedAlbum
import com.dzirbel.kotify.util.flatMapParallel
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import java.time.Instant

object AlbumTable : SpotifyEntityTable(name = "albums") {
    val albumType: Column<SpotifyAlbum.Type?> = enumeration("album_type", SpotifyAlbum.Type::class).nullable()
    val releaseDate: Column<String?> = text("release_date").nullable()
    val releaseDatePrecision: Column<String?> = text("release_date_precision").nullable()
    val totalTracks: Column<Int?> = integer("total_tracks").nullable()
    val label: Column<String?> = text("label").nullable()
    val popularity: Column<Int?> = integer("popularity").nullable()

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

    object SavedAlbumsTable : SavedEntityTable(name = "saved_albums")
}

class Album(id: EntityID<String>) : SpotifyEntity(id = id, table = AlbumTable) {
    var albumType: SpotifyAlbum.Type? by AlbumTable.albumType
    var releaseDate: String? by AlbumTable.releaseDate
    var releaseDatePrecision: String? by AlbumTable.releaseDatePrecision
    var totalTracks: Int? by AlbumTable.totalTracks
    var label: String? by AlbumTable.label
    var popularity: Int? by AlbumTable.popularity

    val artists: ReadWriteCachedProperty<List<Artist>> by (Artist via AlbumTable.AlbumArtistTable).cachedAsList()
    val images: ReadWriteCachedProperty<List<Image>> by (Image via AlbumTable.AlbumImageTable).cachedAsList()
    val genres: ReadWriteCachedProperty<List<Genre>> by (Genre via AlbumTable.AlbumGenreTable).cachedAsList()
    val tracks: ReadWriteCachedProperty<List<Track>> by (Track via AlbumTable.AlbumTrackTable).cachedAsList()

    val parsedReleaseDate: ReleaseDate? by lazy {
        releaseDate?.let { ReleaseDate.parse(it) }
    }

    /**
     * IDs of the tracks on this album; not guaranteed to contain all the tracks, just the ones in
     * [AlbumTable.AlbumTrackTable].
     */
    val trackIds: ReadOnlyCachedProperty<List<String>> = ReadOnlyCachedProperty {
        AlbumTable.AlbumTrackTable
            .select { AlbumTable.AlbumTrackTable.album eq id }
            .map { it[AlbumTable.AlbumTrackTable.track].value }
    }

    val largestImage: ReadOnlyCachedProperty<Image?> by (Image via AlbumTable.AlbumImageTable)
        .cachedReadOnly { it.largest() }

    /**
     * Whether all the tracks on this album (or the expected number of tracks) are in the database. Must be called from
     * within a transaction.
     */
    val hasAllTracks: Boolean
        get() = totalTracks?.let { tracks.live.size == it } == true

    suspend fun getAllTracks(): List<Track> {
        val cachedTracks = KotifyDatabase.transaction("load album $name tracks") {
            totalTracks?.let { totalTracks ->
                tracks.live.takeIf { it.size == totalTracks }
            }
        }
        cachedTracks?.let { return it }

        val networkTracks = Spotify.Albums.getAlbumTracks(id = id.value)
            .fetchAll<SimplifiedSpotifyTrack>()

        return KotifyDatabase.transaction("set album $name tracks") {
            networkTracks.mapNotNull { Track.from(it) }
                .also { tracks.set(it) }
        }
    }

    companion object : SpotifyEntityClass<Album, SpotifyAlbum>(AlbumTable) {
        override fun Album.update(networkModel: SpotifyAlbum) {
            albumType = networkModel.albumType
            releaseDate = networkModel.releaseDate
            releaseDatePrecision = networkModel.releaseDatePrecision
            networkModel.totalTracks?.let {
                totalTracks = it
            }

            artists.set(networkModel.artists.mapNotNull { Artist.from(it) })
            images.set(networkModel.images.map { Image.from(it) })

            if (networkModel is FullSpotifyAlbum) {
                fullUpdatedTime = Instant.now()

                label = networkModel.label
                popularity = networkModel.popularity
                totalTracks = networkModel.tracks.total

                genres.set(networkModel.genres.map { Genre.from(it) })
                tracks.set(networkModel.tracks.items.mapNotNull { Track.from(it) })
            }
        }
    }
}

object AlbumRepository : DatabaseRepository<Album, SpotifyAlbum>(Album) {
    // most batched calls have a maximum of 50; for albums the maximum is 20
    private const val MAX_ALBUM_IDS_LOOKUP = 20

    override suspend fun fetch(id: String) = Spotify.Albums.getAlbum(id = id)
    override suspend fun fetch(ids: List<String>): List<SpotifyAlbum?> {
        return ids.chunked(size = MAX_ALBUM_IDS_LOOKUP)
            .flatMapParallel { idsChunk -> Spotify.Albums.getAlbums(ids = idsChunk) }
    }
}

object SavedAlbumRepository : SavedDatabaseRepository<SpotifySavedAlbum>(
    entityName = "album",
    savedEntityTable = AlbumTable.SavedAlbumsTable,
) {
    override suspend fun fetchIsSaved(ids: List<String>): List<Boolean> {
        return Spotify.Library.checkAlbums(ids = ids)
    }

    override suspend fun pushSaved(ids: List<String>, saved: Boolean) {
        if (saved) Spotify.Library.saveAlbums(ids) else Spotify.Library.removeAlbums(ids)
    }

    override suspend fun fetchLibrary(): Iterable<SpotifySavedAlbum> {
        return Spotify.Library
            .getSavedAlbums(limit = Spotify.MAX_LIMIT)
            .fetchAll<SpotifySavedAlbum>()
    }

    override fun from(savedNetworkType: SpotifySavedAlbum): String? {
        return Album.from(savedNetworkType.album)?.id?.value
    }
}
