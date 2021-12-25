package com.dzirbel.kotify.db.model

import com.dzirbel.kotify.db.Repository
import com.dzirbel.kotify.db.SpotifyEntity
import com.dzirbel.kotify.db.SpotifyEntityClass
import com.dzirbel.kotify.db.SpotifyEntityTable
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.FullSpotifyArtist
import com.dzirbel.kotify.network.model.SpotifyArtist
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.Table

object ArtistTable : SpotifyEntityTable(name = "artists") {
    val popularity: Column<UInt?> = uinteger("popularity").nullable()
    val followersTotal: Column<UInt?> = uinteger("followers_total").nullable()

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
    var images: SizedIterable<Image> by Image via ArtistTable.ArtistImageTable
    var genres: SizedIterable<Genre> by Genre via ArtistTable.ArtistGenreTable

    companion object : SpotifyEntityClass<Artist, SpotifyArtist>(ArtistTable) {
        override fun Artist.update(networkModel: SpotifyArtist) {
            if (networkModel is FullSpotifyArtist) {
                popularity = networkModel.popularity.toUInt()
                followersTotal = networkModel.followers.total.toUInt()
                images = SizedCollection(networkModel.images.map { Image.from(it) })
                genres = SizedCollection(networkModel.genres.map { Genre.from(it) })
            }
        }
    }
}

object ArtistRepository : Repository<Artist, SpotifyArtist>(Artist) {
    override suspend fun fetch(id: String) = Spotify.Artists.getArtist(id = id)
    override suspend fun fetch(ids: List<String>) = Spotify.Artists.getArtists(ids = ids)
}
