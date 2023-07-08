package com.dzirbel.kotify.repository2.album

import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.SpotifyAlbum
import com.dzirbel.kotify.repository2.DatabaseRepository
import com.dzirbel.kotify.util.flatMapParallel

// most batched calls have a maximum of 50; for albums the maximum is 20
private const val MAX_ALBUM_IDS_LOOKUP = 20

object AlbumRepository : DatabaseRepository<Album, SpotifyAlbum>(Album) {
    override suspend fun fetch(id: String) = Spotify.Albums.getAlbum(id = id)
    override suspend fun fetch(ids: List<String>): List<SpotifyAlbum?> {
        return ids.chunked(size = MAX_ALBUM_IDS_LOOKUP)
            .flatMapParallel { idsChunk -> Spotify.Albums.getAlbums(ids = idsChunk) }
    }
}
