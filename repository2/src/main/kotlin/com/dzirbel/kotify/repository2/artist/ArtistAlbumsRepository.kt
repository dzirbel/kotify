package com.dzirbel.kotify.repository2.artist

import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.ArtistAlbum
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.SimplifiedSpotifyAlbum
import com.dzirbel.kotify.network.model.asFlow
import com.dzirbel.kotify.repository2.DatabaseRepository
import com.dzirbel.kotify.repository2.Repository
import com.dzirbel.kotify.repository2.album.AlbumRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.toList
import java.time.Instant

open class ArtistAlbumsRepository internal constructor(scope: CoroutineScope) :
    DatabaseRepository<List<ArtistAlbum>, List<SimplifiedSpotifyAlbum>>(entityName = "artist albums", scope = scope) {

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

    override fun convert(id: String, networkModel: List<SimplifiedSpotifyAlbum>): List<ArtistAlbum> {
        return networkModel.mapNotNull { artistAlbum ->
            artistAlbum.id?.let { albumId ->
                val album = AlbumRepository.convert(id = albumId, networkModel = artistAlbum)

                // TODO ArtistAlbum may have multiple artists
                ArtistAlbum.from(artistId = id, albumId = album.id.value, albumGroup = artistAlbum.albumGroup).also {
                    // TODO loadToCache
                    it.album.loadToCache()
                }
            }
        }
    }

    companion object : ArtistAlbumsRepository(scope = Repository.applicationScope)
}
