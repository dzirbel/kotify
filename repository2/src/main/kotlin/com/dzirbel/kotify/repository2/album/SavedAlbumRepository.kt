package com.dzirbel.kotify.repository2.album

import com.dzirbel.kotify.db.model.AlbumTable
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.SpotifySavedAlbum
import com.dzirbel.kotify.network.model.asFlow
import com.dzirbel.kotify.repository2.DatabaseSavedRepository
import com.dzirbel.kotify.util.flatMapParallel
import com.dzirbel.kotify.util.parseInstantOrNull
import kotlinx.coroutines.flow.toList
import java.time.Instant

object SavedAlbumRepository : DatabaseSavedRepository<SpotifySavedAlbum>(AlbumTable.SavedAlbumsTable) {
    override suspend fun fetchIsSaved(ids: List<String>): List<Boolean> {
        return ids.chunked(size = Spotify.MAX_LIMIT).flatMapParallel { chunk ->
            Spotify.Library.checkAlbums(ids = chunk)
        }
    }

    override suspend fun pushSaved(ids: List<String>, saved: Boolean) {
        if (saved) Spotify.Library.saveAlbums(ids) else Spotify.Library.removeAlbums(ids)
    }

    override suspend fun fetchLibrary(): Iterable<SpotifySavedAlbum> {
        return Spotify.Library.getSavedAlbums(limit = Spotify.MAX_LIMIT).asFlow().toList()
    }

    override fun convert(savedNetworkType: SpotifySavedAlbum): Pair<String, Instant?> {
        val album = savedNetworkType.album
        AlbumRepository.convert(id = album.id, networkModel = album)
        return album.id to parseInstantOrNull(savedNetworkType.addedAt)
    }
}
