package com.dzirbel.kotify.repository.playlist

import com.dzirbel.kotify.db.model.Image
import com.dzirbel.kotify.db.model.Playlist
import com.dzirbel.kotify.db.util.sized
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.FullSpotifyPlaylist
import com.dzirbel.kotify.network.model.SimplifiedSpotifyPlaylist
import com.dzirbel.kotify.network.model.SpotifyPlaylist
import com.dzirbel.kotify.repository.ConvertingRepository
import com.dzirbel.kotify.repository.DatabaseEntityRepository
import com.dzirbel.kotify.repository.Repository
import com.dzirbel.kotify.repository.user.UserRepository
import com.dzirbel.kotify.repository.user.convertToDB
import com.dzirbel.kotify.repository.util.updateOrInsert
import kotlinx.coroutines.CoroutineScope
import java.time.Instant

interface PlaylistRepository : Repository<PlaylistViewModel>, ConvertingRepository<Playlist, SpotifyPlaylist>

class DatabasePlaylistRepository(
    scope: CoroutineScope,
    private val playlistTracksRepository: PlaylistTracksRepository,
    private val userRepository: UserRepository,
) : DatabaseEntityRepository<PlaylistViewModel, Playlist, SpotifyPlaylist>(entityClass = Playlist, scope = scope),
    PlaylistRepository {

    override suspend fun fetchFromRemote(id: String) = Spotify.Playlists.getPlaylist(playlistId = id)

    override fun convertToDB(id: String, networkModel: SpotifyPlaylist, fetchTime: Instant): Playlist {
        return Playlist.updateOrInsert(id = id, networkModel = networkModel, fetchTime = fetchTime) {
            collaborative = networkModel.collaborative
            networkModel.description?.let { description = it }
            networkModel.public?.let { public = it }
            snapshotId = networkModel.snapshotId

            owner = userRepository.convertToDB(networkModel.owner, fetchTime)

            images = networkModel.images
                .orEmpty()
                .map { Image.findOrCreate(url = it.url, width = it.width, height = it.height) }
                .sized()

            if (networkModel is SimplifiedSpotifyPlaylist) {
                networkModel.tracks?.let {
                    totalTracks = it.total
                }
            }

            if (networkModel is FullSpotifyPlaylist) {
                fullUpdatedTime = fetchTime
                followersTotal = networkModel.followers.total

                totalTracks = networkModel.tracks.total

                tracksFetched = fetchTime
                networkModel.tracks.items.mapIndexedNotNull { index, track ->
                    playlistTracksRepository.convertTrack(
                        spotifyPlaylistTrack = track,
                        playlistId = networkModel.id,
                        index = index,
                        fetchTime = fetchTime,
                    )
                }
            }
        }
    }

    override fun convertToVM(databaseModel: Playlist, fetchTime: Instant) = PlaylistViewModel(databaseModel)
}
