package com.dzirbel.kotify.repository2.playlist

import com.dzirbel.kotify.db.model.Image
import com.dzirbel.kotify.db.model.Playlist
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.FullSpotifyPlaylist
import com.dzirbel.kotify.network.model.SimplifiedSpotifyPlaylist
import com.dzirbel.kotify.network.model.SpotifyPlaylist
import com.dzirbel.kotify.repository2.DatabaseEntityRepository
import com.dzirbel.kotify.repository2.user.UserRepository
import java.time.Instant

object PlaylistRepository : DatabaseEntityRepository<Playlist, SpotifyPlaylist>(Playlist) {
    override suspend fun fetchFromRemote(id: String) = Spotify.Playlists.getPlaylist(playlistId = id)

    override fun convert(id: String, networkModel: SpotifyPlaylist): Playlist {
        return Playlist.updateOrInsert(id = id, networkModel = networkModel) {
            collaborative = networkModel.collaborative
            networkModel.description?.let { description = it }
            networkModel.public?.let { public = it }
            snapshotId = networkModel.snapshotId

            owner.set(UserRepository.convert(networkModel.owner.id, networkModel.owner))

            images.set(networkModel.images.map { Image.from(it) })

            if (networkModel is SimplifiedSpotifyPlaylist) {
                networkModel.tracks?.let {
                    totalTracks = it.total
                }
            }

            if (networkModel is FullSpotifyPlaylist) {
                fullUpdatedTime = Instant.now()
                followersTotal = networkModel.followers.total

                totalTracks = networkModel.tracks.total

                networkModel.tracks.items.mapIndexedNotNull { index, track ->
                    PlaylistTracksRepository.convertTrack(
                        spotifyPlaylistTrack = track,
                        playlistId = networkModel.id,
                        index = index,
                    )
                }
            }
        }
    }
}
