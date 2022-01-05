package com.dzirbel.kotify.db.model

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.Repository
import com.dzirbel.kotify.db.SavableSpotifyEntity
import com.dzirbel.kotify.db.SavedEntityTable
import com.dzirbel.kotify.db.SavedRepository
import com.dzirbel.kotify.db.SpotifyEntityClass
import com.dzirbel.kotify.db.SpotifyEntityTable
import com.dzirbel.kotify.db.cachedAsList
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.FullSpotifyAlbum
import com.dzirbel.kotify.network.model.SimplifiedSpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyAlbum
import com.dzirbel.kotify.network.model.SpotifySavedAlbum
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
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

    object SavedAlbumsTable : SavedEntityTable(name = "saved_albums")
}

class Album(id: EntityID<String>) : SavableSpotifyEntity(
    id = id,
    table = AlbumTable,
    savedEntityTable = AlbumTable.SavedAlbumsTable,
    globalUpdateKey = GlobalUpdateTimesTable.Keys.SAVED_ALBUMS,
) {
    var albumType: SpotifyAlbum.Type? by AlbumTable.albumType
    var releaseDate: String? by AlbumTable.releaseDate
    var releaseDatePrecision: String? by AlbumTable.releaseDatePrecision
    var totalTracks: UInt? by AlbumTable.totalTracks
    var label: String? by AlbumTable.label
    var popularity: UInt? by AlbumTable.popularity

    var artists: List<Artist> by (Artist via AlbumTable.AlbumArtistTable).cachedAsList()
    var images: List<Image> by (Image via AlbumTable.AlbumImageTable).cachedAsList()
    var genres: List<Genre> by (Genre via AlbumTable.AlbumGenreTable).cachedAsList()
    var tracks: List<Track> by (Track via AlbumTable.AlbumTrackTable).cachedAsList()

    /**
     * Whether all the tracks on this album (or the expected number of tracks) are in the database. Must be called from
     * within a transaction.
     */
    val hasAllTracks: Boolean
        get() = totalTracks?.let { tracks.size.toUInt() == it } == true

    suspend fun getAllTracks(): List<Track> {
        return if (hasAllTracks) {
            tracks
        } else {
            val networkTracks = Spotify.Albums.getAlbumTracks(id = id.value)
                .fetchAll<SimplifiedSpotifyTrack>()

            KotifyDatabase.transaction {
                networkTracks.mapNotNull { Track.from(it) }
                    .also { tracks = it }
            }
        }
    }

    companion object : SpotifyEntityClass<Album, SpotifyAlbum>(AlbumTable) {
        fun fromSavedAlbum(spotifySavedAlbum: SpotifySavedAlbum): Album? {
            val album = from(spotifySavedAlbum.album)
            album?.setSaved(saved = true, saveTime = Instant.parse(spotifySavedAlbum.addedAt))
            return album
        }

        override fun Album.update(networkModel: SpotifyAlbum) {
            albumType = networkModel.albumType
            releaseDate = networkModel.releaseDate
            releaseDatePrecision = networkModel.releaseDatePrecision
            networkModel.totalTracks?.toUInt()?.let {
                totalTracks = it
            }

            artists = networkModel.artists.mapNotNull { Artist.from(it) }
            images = networkModel.images.map { Image.from(it) }

            if (networkModel is FullSpotifyAlbum) {
                fullUpdatedTime = Instant.now()

                label = networkModel.label
                popularity = networkModel.popularity.toUInt()
                totalTracks = networkModel.tracks.total.toUInt()

                genres = networkModel.genres.map { Genre.from(it) }
                tracks = networkModel.tracks.items.mapNotNull { Track.from(it) }
            }
        }
    }
}

// TODO move saved functions to common logic for all saved entities?
// TODO make the SAVED_ALBUMS a private variable here to guarantee its wrappers are used?
object AlbumRepository : Repository<Album, SpotifyAlbum>(Album) {
    // most batched calls have a maximum of 50; for albums the maximum is 20
    private const val MAX_ALBUM_IDS_LOOKUP = 20

    override suspend fun fetch(id: String) = Spotify.Albums.getAlbum(id = id)
    override suspend fun fetch(ids: List<String>): List<SpotifyAlbum?> {
        // TODO fetch chunks in parallel
        return ids.chunked(size = MAX_ALBUM_IDS_LOOKUP)
            .flatMap { idsChunk -> Spotify.Albums.getAlbums(ids = idsChunk) }
    }
}

object SavedAlbumRepository : SavedRepository<SpotifySavedAlbum>(savedEntityTable = AlbumTable.SavedAlbumsTable) {
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
        return Album.fromSavedAlbum(savedNetworkType)?.id?.value
    }
}
