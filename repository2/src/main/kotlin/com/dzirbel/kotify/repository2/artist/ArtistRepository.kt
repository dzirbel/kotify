package com.dzirbel.kotify.repository2.artist

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.ArtistAlbum
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.SimplifiedSpotifyAlbum
import com.dzirbel.kotify.network.model.SpotifyArtist
import com.dzirbel.kotify.network.model.asFlow
import com.dzirbel.kotify.repository2.CacheState
import com.dzirbel.kotify.repository2.DatabaseRepository
import com.dzirbel.kotify.util.flatMapParallel
import kotlinx.coroutines.flow.toList
import java.time.Instant

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
        val updateTime = Instant.now()

        return KotifyDatabase.transaction("save artist ${artist?.name ?: "id $artistId"} albums") {
            (artist ?: Artist.findById(id = artistId))?.let { artist ->
                artist.albumsFetched = updateTime
                states.updateValue(artistId, CacheState.Loaded(artist, updateTime))
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
