package com.dzirbel.kotify.repository2.album

import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.ArtistAlbum
import com.dzirbel.kotify.db.model.Genre
import com.dzirbel.kotify.db.model.Image
import com.dzirbel.kotify.db.model.Track
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.FullSpotifyAlbum
import com.dzirbel.kotify.network.model.SpotifyAlbum
import com.dzirbel.kotify.repository2.DatabaseEntityRepository
import com.dzirbel.kotify.repository2.Repository
import com.dzirbel.kotify.util.flatMapParallel
import kotlinx.coroutines.CoroutineScope
import java.time.Instant

// most batched calls have a maximum of 50; for albums the maximum is 20
private const val MAX_ALBUM_IDS_LOOKUP = 20

open class AlbumRepository internal constructor(scope: CoroutineScope) :
    DatabaseEntityRepository<Album, SpotifyAlbum>(entityClass = Album, scope = scope) {

    override suspend fun fetchFromRemote(id: String) = Spotify.Albums.getAlbum(id = id)
    override suspend fun fetchFromRemote(ids: List<String>): List<SpotifyAlbum?> {
        return ids.chunked(size = MAX_ALBUM_IDS_LOOKUP)
            .flatMapParallel { idsChunk -> Spotify.Albums.getAlbums(ids = idsChunk) }
    }

    override fun convert(id: String, networkModel: SpotifyAlbum): Album {
        return Album.updateOrInsert(id = id, networkModel = networkModel) {
            albumType = networkModel.albumType
            releaseDate = networkModel.releaseDate
            releaseDatePrecision = networkModel.releaseDatePrecision
            networkModel.totalTracks?.let {
                totalTracks = it
            }

            // attempt to link artists from network model; do not set albumGroup since it is unavailable from an album
            // context
            networkModel.artists.forEach { artistModel ->
                Artist.from(artistModel)?.let { artist ->
                    ArtistAlbum.from(artistId = artist.id.value, albumId = id, albumGroup = null)
                }
            }

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

    companion object : AlbumRepository(scope = Repository.applicationScope)
}
