package com.dzirbel.kotify.db.model

import androidx.compose.runtime.Immutable
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
import com.dzirbel.kotify.network.model.CursorPaging
import com.dzirbel.kotify.network.model.FullSpotifyArtist
import com.dzirbel.kotify.network.model.SimplifiedSpotifyAlbum
import com.dzirbel.kotify.network.model.SpotifyArtist
import com.dzirbel.kotify.network.model.asFlow
import com.dzirbel.kotify.util.flatMapParallel
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Serializable
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

@Immutable
class Artist(id: EntityID<String>) : SpotifyEntity(id = id, table = ArtistTable) {
    var popularity: Int? by ArtistTable.popularity
    var followersTotal: Int? by ArtistTable.followersTotal
    var albumsFetched: Instant? by ArtistTable.albumsFetched

    val images: ReadWriteCachedProperty<List<Image>> by (Image via ArtistTable.ArtistImageTable).cachedAsList()
    val genres: ReadWriteCachedProperty<List<Genre>> by (Genre via ArtistTable.ArtistGenreTable).cachedAsList()

    val artistAlbums: ReadOnlyCachedProperty<List<ArtistAlbum>> by (ArtistAlbum referrersOn ArtistAlbumTable.artist)
        .cachedReadOnly(baseToDerived = { it.toList() })

    val largestImage: ReadOnlyCachedProperty<Image?> by (Image via ArtistTable.ArtistImageTable)
        .cachedReadOnly { it.largest() }

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

object ArtistRepository : DatabaseRepository<Artist, SpotifyArtist>(Artist) {
    override suspend fun fetch(id: String) = Spotify.Artists.getArtist(id = id)
    override suspend fun fetch(ids: List<String>): List<SpotifyArtist?> {
        return ids.chunked(size = Spotify.MAX_LIMIT)
            .flatMapParallel { idsChunk -> Spotify.Artists.getArtists(ids = idsChunk) }
    }

    /**
     * Retrieves the [Album]s by the artist with the given [artistId], from the database if the artist and all their
     * albums are cached or from the network if not, also connecting them in the database along the way.
     */
    suspend fun getAllAlbums(
        artistId: String,
        allowCache: Boolean = true,
        fetchAlbums: suspend () -> List<SimplifiedSpotifyAlbum> = {
            Spotify.Artists.getArtistAlbums(id = artistId).asFlow().toList()
        },
    ): List<ArtistAlbum> {
        var artist: Artist? = null
        if (allowCache) {
            KotifyDatabase.transaction("load artist albums for id $artistId") {
                Artist.findById(id = artistId)
                    ?.also { artist = it }
                    ?.takeIf { it.hasAllAlbums }
                    ?.artistAlbums
                    ?.live
                    ?.onEach { it.album.loadToCache() }
            }
                ?.let { return it }
        }

        val networkAlbums = fetchAlbums()

        return KotifyDatabase.transaction("save artist ${artist?.name ?: "id $artistId"} albums") {
            (artist ?: Artist.findById(id = artistId))?.let { artist ->
                artist.albumsFetched = Instant.now()
                updateLiveState(id = artistId, value = artist)
            }

            networkAlbums
                .mapNotNull { spotifyAlbum ->
                    Album.from(spotifyAlbum)?.let { album ->
                        ArtistAlbum.from(
                            artistId = artistId,
                            albumId = album.id.value,
                            albumGroup = spotifyAlbum.albumGroup,
                        )
                    }
                }
                .onEach { it.album.loadToCache() }
        }
    }
}

object SavedArtistRepository : SavedDatabaseRepository<FullSpotifyArtist>(
    entityName = "artist",
    savedEntityTable = ArtistTable.SavedArtistsTable,
) {
    override suspend fun fetchIsSaved(ids: List<String>): List<Boolean> {
        return ids.chunked(size = Spotify.MAX_LIMIT).flatMapParallel { chunk ->
            Spotify.Follow.isFollowing(type = "artist", ids = chunk)
        }
    }

    override suspend fun pushSaved(ids: List<String>, saved: Boolean) {
        if (saved) {
            Spotify.Follow.follow(type = "artist", ids = ids)
        } else {
            Spotify.Follow.unfollow(type = "artist", ids = ids)
        }
    }

    override suspend fun fetchLibrary(): Iterable<FullSpotifyArtist> {
        @Serializable
        data class ArtistsCursorPagingModel(val artists: CursorPaging<FullSpotifyArtist>)

        return Spotify.Follow.getFollowedArtists(limit = Spotify.MAX_LIMIT)
            .asFlow { url -> Spotify.get<ArtistsCursorPagingModel>(url).artists }
            .toList()
    }

    override fun from(savedNetworkType: FullSpotifyArtist): String? {
        return Artist.from(savedNetworkType)?.id?.value
    }
}
