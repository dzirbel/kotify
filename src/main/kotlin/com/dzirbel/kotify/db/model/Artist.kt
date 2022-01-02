package com.dzirbel.kotify.db.model

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.Repository
import com.dzirbel.kotify.db.SpotifyEntity
import com.dzirbel.kotify.db.SpotifyEntityClass
import com.dzirbel.kotify.db.SpotifyEntityTable
import com.dzirbel.kotify.db.cachedAsList
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.FullSpotifyArtist
import com.dzirbel.kotify.network.model.SimplifiedSpotifyAlbum
import com.dzirbel.kotify.network.model.SpotifyArtist
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object ArtistTable : SpotifyEntityTable(name = "artists") {
    val popularity: Column<UInt?> = uinteger("popularity").nullable()
    val followersTotal: Column<UInt?> = uinteger("followers_total").nullable()
    val albumsFetched: Column<Instant?> = timestamp("albums_fetched_time").nullable()

    object ArtistImageTable : Table() {
        val artist = reference("artist", ArtistTable)
        val image = reference("image", ImageTable)
        override val primaryKey = PrimaryKey(artist, image)
    }

    object ArtistGenreTable : Table() {
        val artist = reference("artist", ArtistTable)
        val genre = reference("genre", GenreTable)
        override val primaryKey = PrimaryKey(artist, genre)
    }
}

class Artist(id: EntityID<String>) : SpotifyEntity(id = id, table = ArtistTable) {
    var popularity: UInt? by ArtistTable.popularity
    var followersTotal: UInt? by ArtistTable.followersTotal
    var albumsFetched: Instant? by ArtistTable.albumsFetched

    var images: List<Image> by (Image via ArtistTable.ArtistImageTable).cachedAsList()
    var genres: List<Genre> by (Genre via ArtistTable.ArtistGenreTable).cachedAsList()
    var albums: List<Album> by (Album via AlbumTable.AlbumArtistTable).cachedAsList()

    val hasAllAlbums: Boolean
        get() = albumsFetched != null

    companion object : SpotifyEntityClass<Artist, SpotifyArtist>(ArtistTable) {
        override fun Artist.update(networkModel: SpotifyArtist) {
            if (networkModel is FullSpotifyArtist) {
                fullUpdatedTime = Instant.now()

                popularity = networkModel.popularity.toUInt()
                followersTotal = networkModel.followers.total.toUInt()
                images = networkModel.images.map { Image.from(it) }
                genres = networkModel.genres.map { Genre.from(it) }
            }
        }

        /**
         * Retrieves the [Album]s by the artist with the given [artistId], from the database if the artist and all their
         * albums are cached or from the network if not, also connecting them in the database along the way.
         */
        suspend fun getAllAlbums(artistId: String): List<Album> {
            val artist = KotifyDatabase.transaction { findById(id = artistId) }
            if (artist?.hasAllAlbums == true) {
                return artist.albums
            }

            val networkAlbums = Spotify.Artists.getArtistAlbums(id = artistId)
                .fetchAll<SimplifiedSpotifyAlbum>()

            return KotifyDatabase.transaction {
                // TODO create artist entity if it does not exist in order to save the albums? unlikely to ever happen
                //  in practice
                networkAlbums
                    .mapNotNull { Album.from(it) }
                    .also {
                        artist?.albums = it
                        artist?.albumsFetched = Instant.now()
                    }
            }
        }
    }
}

object ArtistRepository : Repository<Artist, SpotifyArtist>(Artist) {
    override suspend fun fetch(id: String) = Spotify.Artists.getArtist(id = id)
    override suspend fun fetch(ids: List<String>) = Spotify.Artists.getArtists(ids = ids)
}
