package com.dzirbel.kotify.repository.album

import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.SpotifyAlbum
import com.dzirbel.kotify.repository.DatabaseRepository
import com.dzirbel.kotify.util.flatMapParallel

object AlbumRepository : DatabaseRepository<Album, SpotifyAlbum>(Album) {
    // most batched calls have a maximum of 50; for albums the maximum is 20
    private const val MAX_ALBUM_IDS_LOOKUP = 20

    override suspend fun fetch(id: String) = Spotify.Albums.getAlbum(id = id)
    override suspend fun fetch(ids: List<String>): List<SpotifyAlbum?> {
        return ids.chunked(size = MAX_ALBUM_IDS_LOOKUP)
            .flatMapParallel { idsChunk -> Spotify.Albums.getAlbums(ids = idsChunk) }
    }
}
