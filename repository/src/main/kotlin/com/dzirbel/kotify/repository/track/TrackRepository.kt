package com.dzirbel.kotify.repository.track

import com.dzirbel.kotify.db.model.Track
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.FullSpotifyTrack
import com.dzirbel.kotify.network.model.SimplifiedSpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyTrack
import com.dzirbel.kotify.repository.ConvertingRepository
import com.dzirbel.kotify.repository.DatabaseEntityRepository
import com.dzirbel.kotify.repository.Repository
import com.dzirbel.kotify.repository.album.AlbumRepository
import com.dzirbel.kotify.repository.artist.ArtistRepository
import com.dzirbel.kotify.repository.artist.ArtistTracksRepository
import com.dzirbel.kotify.repository.convertToDB
import com.dzirbel.kotify.repository.util.updateOrInsert
import com.dzirbel.kotify.util.coroutines.flatMapParallel
import kotlinx.coroutines.CoroutineScope
import java.time.Instant

interface TrackRepository : Repository<TrackViewModel>, ConvertingRepository<Track, SpotifyTrack>

class DatabaseTrackRepository(
    scope: CoroutineScope,
    albumRepository: Lazy<AlbumRepository>, // lazy to avoid circular dependency
    private val artistRepository: ArtistRepository,
    private val artistTracksRepository: ArtistTracksRepository,
) : DatabaseEntityRepository<TrackViewModel, Track, SpotifyTrack>(entityClass = Track, scope = scope),
    TrackRepository {

    private val albumRepository by albumRepository

    override suspend fun fetchFromRemote(id: String) = Spotify.Tracks.getTrack(id = id)
    override suspend fun fetchFromRemote(ids: List<String>): List<FullSpotifyTrack> {
        return ids.chunked(size = Spotify.MAX_LIMIT)
            .flatMapParallel { idsChunk -> Spotify.Tracks.getTracks(ids = idsChunk) }
    }

    override fun convertToDB(id: String, networkModel: SpotifyTrack, fetchTime: Instant): Track {
        return Track.updateOrInsert(id = id, networkModel = networkModel, fetchTime = fetchTime) {
            discNumber = networkModel.discNumber
            durationMs = networkModel.durationMs
            explicit = networkModel.explicit
            local = networkModel.isLocal
            playable = networkModel.isPlayable
            trackNumber = networkModel.trackNumber
            networkModel.album
                ?.let { albumRepository.convertToDB(it, fetchTime) }
                ?.let { album = it }

            artistTracksRepository.setTrackArtists(
                trackId = id,
                artistIds = networkModel.artists.mapNotNull { artistRepository.convertToDB(it, fetchTime)?.id?.value },
            )

            if (networkModel is SimplifiedSpotifyTrack) {
                networkModel.popularity?.let {
                    popularity = it
                }
            }

            if (networkModel is FullSpotifyTrack) {
                fullUpdatedTime = fetchTime
                popularity = networkModel.popularity
            }
        }
    }

    override fun convertToVM(databaseModel: Track, fetchTime: Instant) = TrackViewModel(databaseModel)
}
