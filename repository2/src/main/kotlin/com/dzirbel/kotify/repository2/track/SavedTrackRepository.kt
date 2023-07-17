package com.dzirbel.kotify.repository2.track

import com.dzirbel.kotify.db.model.TrackTable
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.SpotifySavedTrack
import com.dzirbel.kotify.network.model.asFlow
import com.dzirbel.kotify.repository2.DatabaseSavedRepository
import com.dzirbel.kotify.repository2.Repository
import com.dzirbel.kotify.util.flatMapParallel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.toList
import java.time.Instant

open class SavedTrackRepository(scope: CoroutineScope) :
    DatabaseSavedRepository<SpotifySavedTrack>(savedEntityTable = TrackTable.SavedTracksTable, scope = scope) {

    override suspend fun fetchIsSaved(ids: List<String>): List<Boolean> {
        return ids.chunked(size = Spotify.MAX_LIMIT).flatMapParallel { chunk ->
            Spotify.Library.checkTracks(ids = chunk)
        }
    }

    override suspend fun pushSaved(ids: List<String>, saved: Boolean) {
        if (saved) Spotify.Library.saveTracks(ids) else Spotify.Library.removeTracks(ids)
    }

    override suspend fun fetchLibrary(): Iterable<SpotifySavedTrack> {
        return Spotify.Library.getSavedTracks(limit = Spotify.MAX_LIMIT).asFlow().toList()
    }

    override fun convert(savedNetworkType: SpotifySavedTrack): Pair<String, Instant?> {
        val track = savedNetworkType.track
        TrackRepository.convert(id = track.id, networkModel = track)
        return track.id to null
    }

    companion object : SavedTrackRepository(scope = Repository.userSessionScope)
}
