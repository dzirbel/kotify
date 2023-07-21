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
import com.dzirbel.kotify.network.model.FullSpotifyArtist
import com.dzirbel.kotify.network.model.SpotifyArtist
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.select
import java.time.Instant

object ArtistTable : SpotifyEntityTable(name = "artists") {
    val popularity: Column<Int?> = integer("popularity").nullable()
    val followersTotal: Column<Int?> = integer("followers_total").nullable()
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

    object SavedArtistsTable : SavedEntityTable(name = "saved_artists")
}

class Artist(id: EntityID<String>) : SpotifyEntity(id = id, table = ArtistTable) {
    var popularity: Int? by ArtistTable.popularity
    var followersTotal: Int? by ArtistTable.followersTotal
    var albumsFetched: Instant? by ArtistTable.albumsFetched

    val images: ReadWriteCachedProperty<List<Image>> by (Image via ArtistTable.ArtistImageTable).cachedAsList()
    val genres: ReadWriteCachedProperty<List<Genre>> by (Genre via ArtistTable.ArtistGenreTable).cachedAsList()

    val artistAlbums by (ArtistAlbum referrersOn ArtistAlbumTable.artist)
        .cachedReadOnly(transactionName = "load artist ${id.value} albums") { it.toList() }

    val largestImage: TransactionReadOnlyCachedProperty<Image?> by (Image via ArtistTable.ArtistImageTable)
        .cachedReadOnly(transactionName = "load artist ${id.value} largest image") { it.largest() }

    /**
     * IDs of the tracks by this artist; not guaranteed to contain all the tracks, just the ones in
     * [TrackTable.TrackArtistTable].
     */
    val trackIds: ReadOnlyCachedProperty<List<String>> = ReadOnlyCachedProperty {
        TrackTable.TrackArtistTable
            .select { TrackTable.TrackArtistTable.artist eq id }
            .map { it[TrackTable.TrackArtistTable.track].value }
    }

    val hasAllAlbums: Boolean
        get() = albumsFetched != null

    companion object : SpotifyEntityClass<Artist, SpotifyArtist>(ArtistTable) {
        override fun Artist.update(networkModel: SpotifyArtist) {
            if (networkModel is FullSpotifyArtist) {
                fullUpdatedTime = Instant.now()

                popularity = networkModel.popularity
                followersTotal = networkModel.followers.total
                images.set(networkModel.images.map { Image.from(it) })
                genres.set(networkModel.genres.map { Genre.from(it) })
            }
        }
    }
}
