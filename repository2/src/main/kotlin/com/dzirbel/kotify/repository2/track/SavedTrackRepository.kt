package com.dzirbel.kotify.repository2.track

import com.dzirbel.kotify.db.model.Track
import com.dzirbel.kotify.db.model.TrackTable
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.SpotifySavedTrack
import com.dzirbel.kotify.network.model.asFlow
import com.dzirbel.kotify.repository2.DatabaseSavedRepository
import com.dzirbel.kotify.repository2.SavedRepository
import com.dzirbel.kotify.util.flatMapParallel
import kotlinx.coroutines.flow.toList

object SavedTrackRepository : SavedRepository by object : DatabaseSavedRepository<SpotifySavedTrack>(
    savedEntityTable = TrackTable.SavedTracksTable,
) {
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

    override fun from(savedNetworkType: SpotifySavedTrack): String? {
        return Track.from(savedNetworkType.track)?.id?.value
    }
}
