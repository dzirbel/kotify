package com.dzirbel.kotify.repository2.album

import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.AlbumTable
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.SpotifySavedAlbum
import com.dzirbel.kotify.network.model.asFlow
import com.dzirbel.kotify.repository2.DatabaseSavedRepository
import com.dzirbel.kotify.repository2.SavedRepository
import com.dzirbel.kotify.util.flatMapParallel
import kotlinx.coroutines.flow.toList

object SavedAlbumRepository : SavedRepository by object : DatabaseSavedRepository<SpotifySavedAlbum>(
    savedEntityTable = AlbumTable.SavedAlbumsTable,
) {
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

    override fun from(savedNetworkType: SpotifySavedAlbum): String? {
        return Album.from(savedNetworkType.album)?.id?.value
    }
}
