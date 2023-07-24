package com.dzirbel.kotify.repository.album

import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.Track
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.SimplifiedSpotifyTrack
import com.dzirbel.kotify.network.model.asFlow
import com.dzirbel.kotify.repository.DatabaseRepository
import com.dzirbel.kotify.repository.Repository
import com.dzirbel.kotify.repository.track.TrackRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.toList
import java.time.Instant

open class AlbumTracksRepository internal constructor(scope: CoroutineScope) :
    DatabaseRepository<List<Track>, List<SimplifiedSpotifyTrack>>(entityName = "album tracks", scope = scope) {

    override suspend fun fetchFromRemote(id: String): List<SimplifiedSpotifyTrack> {
        return Spotify.Albums.getAlbumTracks(id = id).asFlow().toList()
    }

    override fun fetchFromDatabase(id: String): Pair<List<Track>, Instant>? {
        return Album.findById(id)?.let { album ->
            album.tracksFetched?.let { tracksFetched ->
                val tracks = album.tracks.live.takeIf { it.size == album.totalTracks }
                tracks?.let { Pair(it, tracksFetched) }
            }
        }
    }

    override fun convert(id: String, networkModel: List<SimplifiedSpotifyTrack>): List<Track> {
        // TODO do not ignore tracks with null id
        val tracks = networkModel.mapNotNull { track ->
            track.id?.let { id -> TrackRepository.convert(id, track) }
        }

        Album.findById(id)?.let { album ->
            album.tracks.set(tracks)
            album.tracksFetched = Instant.now() // TODO take as input?
        }

        return tracks
    }

    companion object : AlbumTracksRepository(scope = Repository.applicationScope)
}
