package com.dzirbel.kotify.repository.album

import com.dzirbel.kotify.db.model.AlbumTable
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.SpotifySavedAlbum
import com.dzirbel.kotify.network.model.asFlow
import com.dzirbel.kotify.repository.DatabaseSavedRepository
import com.dzirbel.kotify.repository.Repository
import com.dzirbel.kotify.util.coroutines.flatMapParallel
import com.dzirbel.kotify.util.time.parseInstantOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.toList
import java.time.Instant

open class SavedAlbumRepository internal constructor(scope: CoroutineScope) :
    DatabaseSavedRepository<SpotifySavedAlbum>(savedEntityTable = AlbumTable.SavedAlbumsTable, scope = scope) {

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
        AlbumRepository.convertToDB(networkModel = album)
        return album.id to parseInstantOrNull(savedNetworkType.addedAt)
    }

    companion object : SavedAlbumRepository(scope = Repository.userSessionScope)
}
