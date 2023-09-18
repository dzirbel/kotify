package com.dzirbel.kotify.repository.track

import com.dzirbel.kotify.db.model.TrackTable
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.SpotifySavedTrack
import com.dzirbel.kotify.network.model.asFlow
import com.dzirbel.kotify.repository.DatabaseSavedRepository
import com.dzirbel.kotify.repository.SavedRepository
import com.dzirbel.kotify.repository.convertToDB
import com.dzirbel.kotify.repository.user.UserRepository
import com.dzirbel.kotify.util.coroutines.flatMapParallel
import com.dzirbel.kotify.util.time.parseInstantOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.toList
import java.time.Instant

interface SavedTrackRepository : SavedRepository

class DatabaseSavedTrackRepository(
    scope: CoroutineScope,
    userRepository: UserRepository,
    private val trackRepository: TrackRepository,
) :
    DatabaseSavedRepository<SpotifySavedTrack>(
        savedEntityTable = TrackTable.SavedTracksTable,
        scope = scope,
        userRepository = userRepository,
    ),
    SavedTrackRepository {

    override suspend fun fetchIsSaved(id: String): Boolean {
        return Spotify.Library.checkTracks(ids = listOf(id)).first()
    }

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

    override fun convertToDB(savedNetworkType: SpotifySavedTrack, fetchTime: Instant): Pair<String, Instant?> {
        val trackId = savedNetworkType.track.id
        trackRepository.convertToDB(networkModel = savedNetworkType.track, fetchTime = fetchTime)?.let { track ->
            trackRepository.update(id = trackId, model = track, fetchTime = fetchTime)
        }
        return trackId to parseInstantOrNull(savedNetworkType.addedAt)
    }
}
