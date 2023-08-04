package com.dzirbel.kotify.repository.artist

import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.ArtistAlbum
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.SimplifiedSpotifyAlbum
import com.dzirbel.kotify.network.model.asFlow
import com.dzirbel.kotify.repository.DatabaseRepository
import com.dzirbel.kotify.repository.Repository
import com.dzirbel.kotify.repository.album.AlbumRepository
import com.dzirbel.kotify.repository.album.toAlbumType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.toList
import java.time.Instant

open class ArtistAlbumsRepository internal constructor(
    scope: CoroutineScope,
    private val albumRepository: AlbumRepository,
) : DatabaseRepository<List<ArtistAlbum>, List<ArtistAlbum>, List<SimplifiedSpotifyAlbum>>(
    entityName = "artist albums",
    scope = scope,
) {

    override suspend fun fetchFromRemote(id: String): List<SimplifiedSpotifyAlbum> {
        return Spotify.Artists.getArtistAlbums(id = id).asFlow().toList()
    }

    override fun fetchFromDatabase(id: String): Pair<List<ArtistAlbum>, Instant>? {
        return Artist.findById(id)?.let { artist ->
            artist.albumsFetched?.let { albumsFetched ->
                val artistAlbums = artist.artistAlbums.live.onEach {
                    // TODO loadToCache
                    it.album.loadToCache()
                }
                Pair(artistAlbums, albumsFetched)
            }
        }
    }

    override fun convertToDB(id: String, networkModel: List<SimplifiedSpotifyAlbum>): List<ArtistAlbum> {
        return networkModel.mapNotNull { artistAlbum ->
            albumRepository.convertToDB(artistAlbum)?.let { album ->
                // TODO ArtistAlbum may have multiple artists
                ArtistAlbum.findOrCreate(
                    artistId = id,
                    albumId = album.id.value,
                    albumGroup = artistAlbum.albumGroup?.toAlbumType(),
                ).also {
                    // TODO loadToCache
                    it.album.loadToCache()
                }
            }
        }
    }

    override fun convertToVM(databaseModel: List<ArtistAlbum>) = databaseModel

    companion object : ArtistAlbumsRepository(scope = Repository.applicationScope, albumRepository = AlbumRepository)
}
