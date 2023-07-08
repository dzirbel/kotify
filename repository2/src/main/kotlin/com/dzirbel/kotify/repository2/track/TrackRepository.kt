package com.dzirbel.kotify.repository2.track

import com.dzirbel.kotify.db.model.Track
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.FullSpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyTrack
import com.dzirbel.kotify.repository2.DatabaseRepository
import com.dzirbel.kotify.util.flatMapParallel

object TrackRepository : DatabaseRepository<Track, SpotifyTrack>(Track) {
    override suspend fun fetch(id: String) = Spotify.Tracks.getTrack(id = id)
    override suspend fun fetch(ids: List<String>): List<FullSpotifyTrack> {
        return ids.chunked(size = Spotify.MAX_LIMIT)
            .flatMapParallel { idsChunk -> Spotify.Tracks.getTracks(ids = idsChunk) }
    }
}
