package com.dzirbel.kotify.repository.artist

import com.dzirbel.kotify.db.model.ArtistAlbum
import com.dzirbel.kotify.db.model.ArtistAlbumTable
import com.dzirbel.kotify.db.model.ArtistTable
import com.dzirbel.kotify.db.util.single
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.SimplifiedSpotifyAlbum
import com.dzirbel.kotify.network.model.asFlow
import com.dzirbel.kotify.repository.CacheStrategy
import com.dzirbel.kotify.repository.ConvertingRepository
import com.dzirbel.kotify.repository.DatabaseRepository
import com.dzirbel.kotify.repository.Repository
import com.dzirbel.kotify.repository.album.AlbumRepository
import com.dzirbel.kotify.repository.album.toAlbumType
import com.dzirbel.kotify.repository.convertToDB
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.sql.update
import java.time.Instant

interface ArtistAlbumsRepository :
    Repository<ArtistAlbumsViewModel>,
    ConvertingRepository<List<ArtistAlbum>, List<SimplifiedSpotifyAlbum>>

class DatabaseArtistAlbumsRepository(scope: CoroutineScope, private val albumRepository: AlbumRepository) :
    DatabaseRepository<ArtistAlbumsViewModel, List<ArtistAlbum>, List<SimplifiedSpotifyAlbum>>(
        entityName = "artist albums",
        entityNamePlural = "artists albums",
        scope = scope,
    ),
    ArtistAlbumsRepository {

    override val defaultCacheStrategy = CacheStrategy.TTL<ArtistAlbumsViewModel> { it.updateTime }

    override suspend fun fetchFromRemote(id: String): List<SimplifiedSpotifyAlbum> {
        return Spotify.Artists.getArtistAlbums(id = id, limit = Spotify.MAX_LIMIT).asFlow().toList()
    }

    override fun fetchFromDatabase(id: String): Pair<List<ArtistAlbum>, Instant>? {
        val albumsFetched = ArtistTable.single(ArtistTable.albumsFetched) { ArtistTable.id eq id } ?: return null
        val artistAlbums = ArtistAlbum.find { ArtistAlbumTable.artist eq id }.toList()
        return Pair(artistAlbums, albumsFetched)
    }

    override fun convertToDB(
        id: String,
        networkModel: List<SimplifiedSpotifyAlbum>,
        fetchTime: Instant,
    ): List<ArtistAlbum> {
        // TODO update album fetch time even when artist is not in DB
        ArtistTable.update(where = { ArtistTable.id eq id }) { it[albumsFetched] = fetchTime }

        return networkModel.mapNotNull { networkAlbum: SimplifiedSpotifyAlbum ->
            albumRepository.convertToDB(networkAlbum, fetchTime)
                ?.also { albumRepository.update(id = it.id.value, model = it, fetchTime = fetchTime) }
                ?.let { album ->
                    // note: do not add ArtistAlbum for other artists on the album, since the albumGroup is specific to
                    // the artist being queried
                    ArtistAlbum.findOrCreate(
                        artistId = id,
                        albumId = album.id.value,
                        albumGroup = networkAlbum.albumGroup?.toAlbumType(),
                    )
                }
        }
    }

    override fun convertToVM(databaseModel: List<ArtistAlbum>, fetchTime: Instant): ArtistAlbumsViewModel {
        return ArtistAlbumsViewModel(
            artistAlbums = databaseModel.map { ArtistAlbumViewModel(it) },
            updateTime = fetchTime,
        )
    }
}
